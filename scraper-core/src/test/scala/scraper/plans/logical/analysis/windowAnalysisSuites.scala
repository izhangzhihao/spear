package scraper.plans.logical.analysis

import scraper.exceptions.IllegalAggregationException
import scraper.expressions._
import scraper.expressions.functions._
import scraper.expressions.windows._
import scraper.plans.logical.LocalRelation

abstract class WindowAnalysisTest extends AnalyzerTest { self =>
  protected val (a, b) = ('a.int.!, 'b.string.?)

  protected val relation = LocalRelation.empty(a, b)

  protected val f0 = WindowFrame(RowsFrame, UnboundedPreceding, CurrentRow)

  protected val f1 = WindowFrame(RangeFrame, Preceding(1), Following(1))

  // -------------------
  // Aggregation aliases
  // -------------------

  protected val `@A: max(a)` = AggregationAlias(max(a))

  protected val `@A: count(b)` = AggregationAlias(count(b))
}

class WindowAnalysisWithoutGroupBySuite extends WindowAnalysisTest { self =>
  test("single window function") {
    checkAnalyzedPlan(
      relation.select('max('a) over `?w0?` as 'win_max),

      relation
        .window(`@W: max(a) over w0`)
        .select(`@W: max(a) over w0`.attr as 'win_max)
    )
  }

  test("single window function with non-window expressions") {
    checkAnalyzedPlan(
      relation.select('a + ('max('a) over `?w0?`) as 'win_max),

      relation
        .window(`@W: max(a) over w0`)
        .select(a + `@W: max(a) over w0`.attr as 'win_max)
    )
  }

  test("multiple window functions with the same window spec") {
    checkAnalyzedPlan(
      relation.select(
        'max('a) over `?w0?` as 'win_max,
        'count('b) over `?w0?` as 'win_count
      ),

      relation.window(
        `@W: max(a) over w0`,
        `@W: count(b) over w0`
      ).select(
        `@W: max(a) over w0`.attr as 'win_max,
        `@W: count(b) over w0`.attr as 'win_count
      )
    )
  }

  test("multiple window functions with the same window spec and non-window expressions") {
    checkAnalyzedPlan(
      relation.select(
        ('a + ('max('a) over `?w0?`)) as 'c0,
        'count('b) over `?w0?` as 'c1
      ),

      relation.window(
        `@W: max(a) over w0`,
        `@W: count(b) over w0`
      ).select(
        a + `@W: max(a) over w0`.attr as 'c0,
        `@W: count(b) over w0`.attr as 'c1
      )
    )
  }

  test("multiple window functions with different window specs") {
    checkAnalyzedPlan(
      relation.select(
        'max('a) over `?w0?` as 'win_max,
        'count('b) over `?w1?` as 'win_count
      ),

      relation
        .window(`@W: max(a) over w0`)
        .window(`@W: count(b) over w1`)
        .select(
          `@W: max(a) over w0`.attr as 'win_max,
          `@W: count(b) over w1`.attr as 'win_count
        )
    )
  }

  test("multiple window functions with different window specs and non-window expressions") {
    checkAnalyzedPlan(
      relation.select(
        ('a + ('max('a) over `?w0?`)) as 'x,
        'count('b) over `?w1?` as 'y
      ),

      relation
        .window(`@W: max(a) over w0`)
        .window(`@W: count(b) over w1`)
        .select(
          a + `@W: max(a) over w0`.attr as 'x,
          `@W: count(b) over w1`.attr as 'y
        )
    )
  }

  test("window function in ORDER BY clause") {
    checkAnalyzedPlan(
      relation
        .orderBy('max('a) over `?w0?`),

      relation
        .window(`@W: max(a) over w0`)
        .orderBy(`@W: max(a) over w0`.attr)
        .select(a, b)
    )
  }

  test("reference window function alias in ORDER BY clause") {
    val win_max = `@W: max(a) over w0`.attr as 'win_max

    checkAnalyzedPlan(
      relation
        .select('max('a) over `?w0?` as 'win_max)
        .orderBy('win_max),

      relation
        .window(`@W: max(a) over w0`)
        .select(win_max)
        .orderBy(win_max.attr)
    )
  }

  // -----------------------
  // Unresolved window specs
  // -----------------------

  private val `?w0?` = Window partitionBy 'a orderBy 'b between f0

  private val `?w1?` = Window partitionBy 'b orderBy 'a between f1

  // ---------------------
  // Resolved window specs
  // ---------------------

  private val w0 = Expression.resolveUsing(relation.output)(`?w0?`)

  private val w1 = Expression.resolveUsing(relation.output)(`?w1?`)

  // --------------
  // Window aliases
  // --------------

  private val `@W: max(a) over w0` = WindowAlias(max(a) over w0)

  private val `@W: count(b) over w0` = WindowAlias(count(b) over w0)

  private val `@W: count(b) over w1` = WindowAlias(count(b) over w1)
}

class WindowAnalysisWithGroupBySuite extends WindowAnalysisTest {
  test("single window function") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg('count('a + 1) over `?w0?` as 'count),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(Nil)
        .window(`@W: count(a + 1) over w0`)
        .select(`@W: count(a + 1) over w0`.attr as 'count)
    )
  }

  test("single window function with non-window aggregate function") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg(
          'count('b) over `?w0?` as 'win_count,
          'count('b) as 'agg_count
        ),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(`@A: count(b)`)
        .window(`@W: count(b) over w1`)
        .select(
          `@W: count(b) over w1`.attr as 'win_count,
          `@A: count(b)`.attr as 'agg_count
        )
    )
  }

  test("multiple window functions with the same window spec") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg(
          'count('a + 1) over `?w0?` as 'count,
          'count('b) over `?w0?` as 'count
        ),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(Nil)
        .window(
          `@W: count(a + 1) over w0`,
          `@W: count(b) over w1`
        )
        .select(
          `@W: count(a + 1) over w0`.attr as 'count,
          `@W: count(b) over w1`.attr as 'count
        )
    )
  }

  test("multiple window functions with multiple window spec") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg(
          'count('a + 1) over `?w0?` as 'count,
          'count('b) over `?w1?` as 'count
        ),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(Nil)
        .window(`@W: count(a + 1) over w0`)
        .window(`@W: count(b) over w2`)
        .select(
          `@W: count(a + 1) over w0`.attr as 'count,
          `@W: count(b) over w2`.attr as 'count
        )
    )
  }

  test("aggregate function inside window spec") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg('count('b) over `?w2?` as 'win_count),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(`@A: max(a)`)
        .window(`@W: count(b) over w3`)
        .select(`@W: count(b) over w3`.attr as 'win_count)
    )
  }

  test("non-window aggregate function inside window aggregate function") {
    val alias = GroupingAlias(a)
    val attr = alias.attr
    attr.sqlLike

    val `@A: avg(a)` = AggregationAlias(avg(a))
    val `@W: max(avg(a)) over w0` = WindowAlias(max(`@A: avg(a)`.attr) over w0)

    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg('max('avg('a)) over `?w0?` as 'win_max_of_avg),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(`@A: avg(a)`)
        .window(`@W: max(avg(a)) over w0`)
        .select(`@W: max(avg(a)) over w0`.attr as 'win_max_of_avg)
    )
  }

  test("window function in ORDER BY clause") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg('a + 1 as 'key)
        .orderBy('count('a + 1) over `?w0?`),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(Nil)
        .window(`@W: count(a + 1) over w0`)
        .orderBy(`@W: count(a + 1) over w0`.attr)
        .select(`@G: a + 1`.attr as 'key)
    )
  }

  test("reference window function alias in ORDER BY clause") {
    checkAnalyzedPlan(
      relation
        .groupBy('a + 1, 'b)
        .agg('count('a + 1) over `?w0?` as 'win_count)
        .orderBy('win_count),

      relation
        .resolvedGroupBy(`@G: a + 1`, `@G: b`)
        .agg(Nil)
        .window(`@W: count(a + 1) over w0`)
        .orderBy(`@W: count(a + 1) over w0`.attr)
        .select(`@W: count(a + 1) over w0`.attr as 'win_count)
    )
  }

  test("illegal window function in HAVING clause") {
    val patterns = Seq("Window functions are not allowed in HAVING clauses.")

    checkMessage[IllegalAggregationException](patterns: _*) {
      analyze(
        relation
          .groupBy('a + 1)
          .agg('a + 1)
          .filter('count('a + 1) over `?w0?`)
      )
    }
  }

  test("illegal window function alias referenced in HAVING clause") {
    val patterns = Seq("Window functions are not allowed in HAVING clauses.")

    checkMessage[IllegalAggregationException](patterns: _*) {
      analyze(
        relation
          .groupBy('a + 1)
          .agg('count('a + 1) over `?w0?` as 'win_count)
          .filter('win_count > 1)
      )
    }
  }

  // ----------------
  // Grouping aliases
  // ----------------

  private val `@G: a + 1` = GroupingAlias(a + 1)

  private val `@G: b` = GroupingAlias(b)

  // -----------------------
  // Unresolved window specs
  // -----------------------

  private val `?w0?` = Window partitionBy 'a + 1 orderBy 'b between f0

  private val `?w1?` = Window partitionBy 'b orderBy 'a + 1 between f1

  private val `?w2?` = Window partitionBy 'max('a)

  // ---------------------
  // Resolved window specs
  // ---------------------

  private val w0 = Window partitionBy `@G: a + 1`.attr orderBy `@G: b`.attr between f0

  private val w1 = Window partitionBy `@G: b`.attr orderBy `@G: a + 1`.attr between f1

  private val w2 = Window partitionBy `@A: max(a)`.attr

  // --------------
  // Window aliases
  // --------------

  private val `@W: count(a + 1) over w0` = WindowAlias(count(`@G: a + 1`.attr) over w0)

  private val `@W: count(b) over w1` = WindowAlias(count(`@G: b`.attr) over w0)

  private val `@W: count(b) over w2` = WindowAlias(count(`@G: b`.attr) over w1)

  private val `@W: count(b) over w3` = WindowAlias(count(`@G: b`.attr) over w2)
}