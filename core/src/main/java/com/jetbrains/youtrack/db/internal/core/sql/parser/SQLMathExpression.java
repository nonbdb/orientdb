/* Generated By:JJTree: Do not edit this line. SQLMathExpression.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.api.exception.BaseException;
import com.jetbrains.youtrack.db.api.exception.CommandExecutionException;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.api.record.Entity;
import com.jetbrains.youtrack.db.api.record.Identifiable;
import com.jetbrains.youtrack.db.api.schema.Collate;
import com.jetbrains.youtrack.db.internal.core.command.CommandContext;
import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.schema.SchemaClassInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.AggregationContext;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import com.jetbrains.youtrack.db.internal.core.sql.executor.metadata.MetadataPath;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLMathExpression extends SimpleNode {

  private static final Object NULL_VALUE = new Object();

  public SQLExpression getExpandContent() {
    throw new CommandExecutionException("Invalid expand expression");
  }

  public boolean isDefinedFor(Result currentRecord) {
    return true;
  }

  public boolean isDefinedFor(DatabaseSessionInternal db, Entity currentRecord) {
    return true;
  }

  public boolean isIndexChain(CommandContext ctx, SchemaClassInternal clazz) {
    return false;
  }

  public enum Operator {
    STAR(10) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left * right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left * right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left * right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left * right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left.multiply(right);
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    SLASH(10) {
      @Override
      public Number apply(Integer left, Integer right) {
        if (left % right == 0) {
          return left / right;
        }
        return ((double) left) / right;
      }

      @Override
      public Number apply(Long left, Long right) {
        if (left % right == 0) {
          return left / right;
        }
        return ((double) left) / right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left / right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left / right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left.divide(right, RoundingMode.HALF_UP);
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    REM(10) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left % right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left % right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left % right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left % right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left.remainder(right);
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    PLUS(20) {
      @Override
      public Number apply(Integer left, Integer right) {
        final Integer sum = left + right;
        if (sum < 0 && left.intValue() > 0 && right.intValue() > 0)
        // SPECIAL CASE: UPGRADE TO LONG
        {
          return left.longValue() + right;
        }
        return sum;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left + right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left + right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left + right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left.add(right);
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null && right == null) {
          return null;
        }
        if (left == null) {
          return right;
        }
        if (right == null) {
          return left;
        }
        if (left instanceof Number && right instanceof Number) {
          return super.apply(left, right);
        }
        if (left instanceof Date || right instanceof Date) {
          var result = apply(toLong(left), toLong(right));
          return new Date(result.longValue());
        }
        return String.valueOf(left) + right;
      }
    },
    MINUS(20) {
      @Override
      public Number apply(Integer left, Integer right) {
        var result = left - right;
        if (result > 0 && left.intValue() < 0 && right.intValue() > 0)
        // SPECIAL CASE: UPGRADE TO LONG
        {
          return left.longValue() - right;
        }

        return result;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left - right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left - right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left - right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left.subtract(right);
      }

      @Override
      public Object apply(Object left, Object right) {
        Object result = null;
        if (left == null && right == null) {
          result = null;
        } else if (left instanceof Number && right == null) {
          result = left;
        } else if (right instanceof Number && left == null) {
          result = apply(0, this, (Number) right);
        } else if (left instanceof Number && right instanceof Number) {
          result = apply((Number) left, this, (Number) right);
        } else if (left instanceof Date || right instanceof Date) {
          var r = apply(toLong(left), toLong(right));
          result = new Date(r.longValue());
        }

        return result;
      }
    },
    LSHIFT(30) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left << right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left << right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return null;
      }

      @Override
      public Number apply(Double left, Double right) {
        return null;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    RSHIFT(30) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left >> right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left >> right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return null;
      }

      @Override
      public Number apply(Double left, Double right) {
        return null;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    RUNSIGNEDSHIFT(30) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left >>> right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left >>> right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return null;
      }

      @Override
      public Number apply(Double left, Double right) {
        return null;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    BIT_AND(40) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left & right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left & right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return apply(left.longValue(), right.longValue());
      }

      @Override
      public Number apply(Double left, Double right) {
        return apply(left.longValue(), right.longValue());
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      public Object apply(Object left, Object right) {
        if (left == null || right == null) {
          return null;
        }
        return super.apply(left, right);
      }
    },
    XOR(50) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left ^ right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left ^ right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return null;
      }

      @Override
      public Number apply(Double left, Double right) {
        return null;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      @Override
      public Object apply(Object left, Object right) {
        if (left == null && right == null) {
          return null;
        }
        if (left instanceof Number && right == null) {
          return apply((Number) left, this, 0);
        }
        if (right instanceof Number && left == null) {
          return apply(0, this, (Number) right);
        }

        if (left instanceof Number && right instanceof Number) {
          return apply((Number) left, this, (Number) right);
        }

        return null;
      }
    },
    BIT_OR(60) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left | right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left | right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return null;
      }

      @Override
      public Number apply(Double left, Double right) {
        return null;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return null;
      }

      public Object apply(Object left, Object right) {
        if (left == null && right == null) {
          return null;
        }
        return super.apply(left == null ? 0 : left, right == null ? 0 : right);
      }
    },
    NULL_COALESCING(25) {
      @Override
      public Number apply(Integer left, Integer right) {
        return left != null ? left : right;
      }

      @Override
      public Number apply(Long left, Long right) {
        return left != null ? left : right;
      }

      @Override
      public Number apply(Float left, Float right) {
        return left != null ? left : right;
      }

      @Override
      public Number apply(Double left, Double right) {
        return left != null ? left : right;
      }

      @Override
      public Number apply(BigDecimal left, BigDecimal right) {
        return left != null ? left : right;
      }

      public Object apply(Object left, Object right) {
        return left != null ? left : right;
      }
    };

    private static Long toLong(Object left) {
      if (left instanceof Number) {
        return ((Number) left).longValue();
      }
      if (left instanceof Date) {
        return ((Date) left).getTime();
      }
      return null;
    }

    private final int priority;

    Operator(int priority) {
      this.priority = priority;
    }

    public abstract Number apply(Integer left, Integer right);

    public abstract Number apply(Long left, Long right);

    public abstract Number apply(Float left, Float right);

    public abstract Number apply(Double left, Double right);

    public abstract Number apply(BigDecimal left, BigDecimal right);

    public Object apply(Object left, Object right) {
      if (left == null) {
        return right;
      }
      if (right == null) {
        return left;
      }
      if (left instanceof Number && right instanceof Number) {
        return apply((Number) left, this, (Number) right);
      }

      return null;
    }

    public Number apply(final Number a, final Operator operation, final Number b) {
      if (a == null || b == null) {
        throw new IllegalArgumentException("Cannot increment a null value");
      }

      if (a instanceof Integer || a instanceof Short) {
        if (b instanceof Integer || b instanceof Short) {
          return operation.apply(a.intValue(), b.intValue());
        } else if (b instanceof Long) {
          return operation.apply(a.longValue(), b.longValue());
        } else if (b instanceof Float) {
          return operation.apply(a.floatValue(), b.floatValue());
        } else if (b instanceof Double) {
          return operation.apply(a.doubleValue(), b.doubleValue());
        } else if (b instanceof BigDecimal) {
          return operation.apply(new BigDecimal((Integer) a), (BigDecimal) b);
        }
      } else if (a instanceof Long) {
        if (b instanceof Integer || b instanceof Long || b instanceof Short) {
          return operation.apply(a.longValue(), b.longValue());
        } else if (b instanceof Float) {
          return operation.apply(a.floatValue(), b.floatValue());
        } else if (b instanceof Double) {
          return operation.apply(a.doubleValue(), b.doubleValue());
        } else if (b instanceof BigDecimal) {
          return operation.apply(new BigDecimal((Long) a), (BigDecimal) b);
        }
      } else if (a instanceof Float) {
        if (b instanceof Short || b instanceof Integer || b instanceof Long || b instanceof Float) {
          return operation.apply(a.floatValue(), b.floatValue());
        } else if (b instanceof Double) {
          return operation.apply(a.doubleValue(), b.doubleValue());
        } else if (b instanceof BigDecimal) {
          return operation.apply(BigDecimal.valueOf((Float) a), (BigDecimal) b);
        }

      } else if (a instanceof Double) {
        if (b instanceof Short
            || b instanceof Integer
            || b instanceof Long
            || b instanceof Float
            || b instanceof Double) {
          return operation.apply(a.doubleValue(), b.doubleValue());
        } else if (b instanceof BigDecimal) {
          return operation.apply(BigDecimal.valueOf((Double) a), (BigDecimal) b);
        }

      } else if (a instanceof BigDecimal) {
        if (b instanceof Integer) {
          return operation.apply((BigDecimal) a, new BigDecimal((Integer) b));
        } else if (b instanceof Long) {
          return operation.apply((BigDecimal) a, new BigDecimal((Long) b));
        } else if (b instanceof Short) {
          return operation.apply((BigDecimal) a, new BigDecimal((Short) b));
        } else if (b instanceof Float) {
          return operation.apply((BigDecimal) a, BigDecimal.valueOf((Float) b));
        } else if (b instanceof Double) {
          return operation.apply((BigDecimal) a, BigDecimal.valueOf((Double) b));
        } else if (b instanceof BigDecimal) {
          return operation.apply((BigDecimal) a, (BigDecimal) b);
        }
      }

      throw new IllegalArgumentException(
          "Cannot increment value '"
              + a
              + "' ("
              + a.getClass()
              + ") with '"
              + b
              + "' ("
              + b.getClass()
              + ")");
    }

    public int getPriority() {
      return priority;
    }
  }

  protected List<SQLMathExpression> childExpressions;
  protected List<Operator> operators;

  public SQLMathExpression(int id) {
    super(id);
  }

  public SQLMathExpression(YouTrackDBSql p, int id) {
    super(p, id);
  }

  public boolean isCacheable(DatabaseSessionInternal session) {
    if (childExpressions != null) {
      for (var exp : childExpressions) {
        if (!exp.isCacheable(session)) {
          return false;
        }
      }
    }
    return true;
  }

  public Object execute(Identifiable iCurrentRecord, CommandContext ctx) {
    if (childExpressions == null || operators == null) {
      return null;
    }

    if (childExpressions.size() == 0) {
      return null;
    }
    if (childExpressions.size() == 1) {
      return childExpressions.get(0).execute(iCurrentRecord, ctx);
    }

    if (childExpressions.size() == 2) {
      var leftValue = childExpressions.get(0).execute(iCurrentRecord, ctx);
      var rightValue = childExpressions.get(1).execute(iCurrentRecord, ctx);
      return operators.get(0).apply(leftValue, rightValue);
    }

    return calculateWithOpPriority(iCurrentRecord, ctx);
  }

  public Object execute(Result iCurrentRecord, CommandContext ctx) {
    if (childExpressions == null || operators == null) {
      return null;
    }
    if (childExpressions.size() == 0) {
      return null;
    }
    if (childExpressions.size() == 1) {
      return childExpressions.get(0).execute(iCurrentRecord, ctx);
    }

    if (childExpressions.size() == 2) {
      var leftValue = childExpressions.get(0).execute(iCurrentRecord, ctx);
      var rightValue = childExpressions.get(1).execute(iCurrentRecord, ctx);
      return operators.get(0).apply(leftValue, rightValue);
    }

    return calculateWithOpPriority(iCurrentRecord, ctx);
  }

  private Object calculateWithOpPriority(Result iCurrentRecord, CommandContext ctx) {
    Deque valuesStack = new ArrayDeque<>();
    Deque<Operator> operatorsStack = new ArrayDeque<Operator>();
    if (childExpressions != null && operators != null) {
      var nextExpression = childExpressions.get(0);
      var val = nextExpression.execute(iCurrentRecord, ctx);
      valuesStack.push(val == null ? NULL_VALUE : val);

      for (var i = 0; i < operators.size() && i + 1 < childExpressions.size(); i++) {
        var nextOperator = operators.get(i);
        var rightValue = childExpressions.get(i + 1).execute(iCurrentRecord, ctx);

        if (!operatorsStack.isEmpty()
            && operatorsStack.peek().getPriority() <= nextOperator.getPriority()) {
          var right = valuesStack.poll();
          right = right == NULL_VALUE ? null : right;
          var left = valuesStack.poll();
          left = left == NULL_VALUE ? null : left;
          var calculatedValue = operatorsStack.poll().apply(left, right);
          valuesStack.push(calculatedValue == null ? NULL_VALUE : calculatedValue);
        }
        operatorsStack.push(nextOperator);

        valuesStack.push(rightValue == null ? NULL_VALUE : rightValue);
      }
    }

    return iterateOnPriorities(valuesStack, operatorsStack);
  }

  private Object calculateWithOpPriority(Identifiable iCurrentRecord, CommandContext ctx) {
    Deque valuesStack = new ArrayDeque<>();
    Deque<Operator> operatorsStack = new ArrayDeque<Operator>();
    if (childExpressions != null && operators != null) {
      var nextExpression = childExpressions.get(0);
      var val = nextExpression.execute(iCurrentRecord, ctx);
      valuesStack.push(val == null ? NULL_VALUE : val);

      for (var i = 0; i < operators.size() && i + 1 < childExpressions.size(); i++) {
        var nextOperator = operators.get(i);
        var rightValue = childExpressions.get(i + 1).execute(iCurrentRecord, ctx);

        if (!operatorsStack.isEmpty()
            && operatorsStack.peek().getPriority() <= nextOperator.getPriority()) {
          var right = valuesStack.poll();
          right = right == NULL_VALUE ? null : right;
          var left = valuesStack.poll();
          left = left == NULL_VALUE ? null : left;
          var calculatedValue = operatorsStack.poll().apply(left, right);
          valuesStack.push(calculatedValue == null ? NULL_VALUE : calculatedValue);
        }
        operatorsStack.push(nextOperator);

        valuesStack.push(rightValue == null ? NULL_VALUE : rightValue);
      }
    }
    return iterateOnPriorities(valuesStack, operatorsStack);
  }

  private Object iterateOnPriorities(Deque values, Deque<Operator> operators) {
    while (true) {
      if (values.size() == 0) {
        return null;
      }
      if (values.size() == 1) {
        return values.getFirst();
      }

      Deque valuesStack = new ArrayDeque<>();
      Deque<SQLMathExpression.Operator> operatorsStack = new ArrayDeque<SQLMathExpression.Operator>();

      valuesStack.push(values.removeLast());

      while (!operators.isEmpty()) {
        var nextOperator = operators.removeLast();
        var rightValue = values.removeLast();

        if (!operatorsStack.isEmpty()
            && operatorsStack.peek().getPriority() <= nextOperator.getPriority()) {
          var right = valuesStack.poll();
          right = right == NULL_VALUE ? null : right;
          var left = valuesStack.poll();
          left = left == NULL_VALUE ? null : left;
          var calculatedValue = operatorsStack.poll().apply(left, right);
          valuesStack.push(calculatedValue == null ? NULL_VALUE : calculatedValue);
        }
        operatorsStack.push(nextOperator);
        valuesStack.push(rightValue == null ? NULL_VALUE : rightValue);
      }
      if (!operatorsStack.isEmpty()) {
        var right = valuesStack.poll();
        right = right == NULL_VALUE ? null : right;
        var left = valuesStack.poll();
        left = left == NULL_VALUE ? null : left;
        var val = operatorsStack.poll().apply(left, right);
        valuesStack.push(val == null ? NULL_VALUE : val);
      }

      values = valuesStack;
      operators = operatorsStack;
    }
  }

  public List<SQLMathExpression> getChildExpressions() {
    return childExpressions;
  }

  public void setChildExpressions(List<SQLMathExpression> childExpressions) {
    this.childExpressions = childExpressions;
  }

  public void addChildExpression(SQLMathExpression expression) {
    if (this.childExpressions == null) {
      this.childExpressions = new ArrayList<>();
    }
    this.childExpressions.add(expression);
  }

  public SQLMathExpression unwrapIfNeeded() {
    if (this.childExpressions != null && this.childExpressions.size() == 1) {
      return this.childExpressions.get(0);
    }
    return this;
  }

  public List<Operator> getOperators() {
    return operators;
  }

  public void addOperator(Operator operator) {
    if (this.operators == null) {
      this.operators = new ArrayList<>();
    }
    this.operators.add(operator);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (childExpressions == null || operators == null) {
      return;
    }
    for (var i = 0; i < childExpressions.size(); i++) {
      if (i > 0) {
        builder.append(" ");
        switch (operators.get(i - 1)) {
          case PLUS:
            builder.append("+");
            break;
          case MINUS:
            builder.append("-");
            break;
          case STAR:
            builder.append("*");
            break;
          case SLASH:
            builder.append("/");
            break;
          case REM:
            builder.append("%");
            break;
          case LSHIFT:
            builder.append("<<");
            break;
          case RSHIFT:
            builder.append(">>");
            break;
          case RUNSIGNEDSHIFT:
            builder.append(">>>");
            break;
          case BIT_AND:
            builder.append("&");
            break;
          case BIT_OR:
            builder.append("|");
            break;
          case XOR:
            builder.append("^");
            break;
        }
        builder.append(" ");
      }
      childExpressions.get(i).toString(params, builder);
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    if (childExpressions == null || operators == null) {
      return;
    }
    for (var i = 0; i < childExpressions.size(); i++) {
      if (i > 0) {
        builder.append(" ");
        switch (operators.get(i - 1)) {
          case PLUS:
            builder.append("+");
            break;
          case MINUS:
            builder.append("-");
            break;
          case STAR:
            builder.append("*");
            break;
          case SLASH:
            builder.append("/");
            break;
          case REM:
            builder.append("%");
            break;
          case LSHIFT:
            builder.append("<<");
            break;
          case RSHIFT:
            builder.append(">>");
            break;
          case RUNSIGNEDSHIFT:
            builder.append(">>>");
            break;
          case BIT_AND:
            builder.append("&");
            break;
          case BIT_OR:
            builder.append("|");
            break;
          case XOR:
            builder.append("^");
            break;
        }
        builder.append(" ");
      }
      childExpressions.get(i).toGenericStatement(builder);
    }
  }

  protected boolean supportsBasicCalculation() {
    if (this.childExpressions != null) {
      for (var expr : this.childExpressions) {
        if (!expr.supportsBasicCalculation()) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean isIndexedFunctionCall(DatabaseSessionInternal session) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions.get(0).isIndexedFunctionCall(session);
      }
    }
    return false;
  }

  public long estimateIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions
            .get(0)
            .estimateIndexedFunction(target, context, operator, right);
      }
    }
    return -1;
  }

  public Iterable<Identifiable> executeIndexedFunction(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions
            .get(0)
            .executeIndexedFunction(target, context, operator, right);
      }
    }
    return null;
  }

  /**
   * tests if current expression is an indexed funciton AND that function can also be executed
   * without using the index
   *
   * @param target  the query target
   * @param context the execution context
   * @return true if current expression is an indexed funciton AND that function can also be
   * executed without using the index, false otherwise
   */
  public boolean canExecuteIndexedFunctionWithoutIndex(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions
            .get(0)
            .canExecuteIndexedFunctionWithoutIndex(target, context, operator, right);
      }
    }
    return false;
  }

  /**
   * tests if current expression is an indexed function AND that function can be used on this
   * target
   *
   * @param target  the query target
   * @param context the execution context
   * @return true if current expression is an indexed function AND that function can be used on this
   * target, false otherwise
   */
  public boolean allowsIndexedFunctionExecutionOnTarget(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions
            .get(0)
            .allowsIndexedFunctionExecutionOnTarget(target, context, operator, right);
      }
    }

    return false;
  }

  /**
   * tests if current expression is an indexed function AND the function has also to be executed
   * after the index search. In some cases, the index search is accurate, so this condition can be
   * excluded from further evaluation. In other cases the result from the index is a superset of the
   * expected result, so the function has to be executed anyway for further filtering
   *
   * @param target  the query target
   * @param context the execution context
   * @return true if current expression is an indexed function AND the function has also to be
   * executed after the index search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(
      SQLFromClause target, CommandContext context, SQLBinaryCompareOperator operator,
      Object right) {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions
            .get(0)
            .executeIndexedFunctionAfterIndexSearch(target, context, operator, right);
      }
    }
    return false;
  }

  public boolean isFunctionAny() {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions.get(0).isFunctionAny();
      }
    }
    return false;
  }

  public boolean isFunctionAll() {
    if (this.childExpressions != null) {
      if (this.childExpressions.size() == 1) {
        return this.childExpressions.get(0).isFunctionAll();
      }
    }
    return false;
  }

  public boolean isBaseIdentifier() {
    if (this.childExpressions != null) {
      if (childExpressions.size() == 1) {
        return childExpressions.get(0).isBaseIdentifier();
      }
    }
    return false;
  }

  public Optional<MetadataPath> getPath() {
    if (this.childExpressions != null) {
      if (childExpressions.size() == 1) {
        return childExpressions.get(0).getPath();
      }
    }
    return Optional.empty();
  }

  public Collate getCollate(Result currentRecord, CommandContext ctx) {
    if (this.childExpressions != null) {
      if (childExpressions.size() == 1) {
        return childExpressions.get(0).getCollate(currentRecord, ctx);
      }
    }
    return null;
  }

  public boolean isEarlyCalculated(CommandContext ctx) {
    if (this.childExpressions != null) {
      for (var exp : childExpressions) {
        if (!exp.isEarlyCalculated(ctx)) {
          return false;
        }
      }
    }
    return true;
  }

  public boolean needsAliases(Set<String> aliases) {
    if (this.childExpressions != null) {
      for (var expr : childExpressions) {
        if (expr.needsAliases(aliases)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isExpand() {
    if (this.childExpressions != null) {

      for (var expr : this.childExpressions) {
        if (expr.isExpand()) {
          if (this.childExpressions.size() > 1) {
            throw new CommandExecutionException(
                "Cannot calculate expand() with other expressions");
          }
          return true;
        }
      }
    }
    return false;
  }

  public boolean isAggregate(DatabaseSessionInternal session) {
    if (this.childExpressions != null) {
      for (var expr : this.childExpressions) {
        if (expr.isAggregate(session)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isCount() {
    if (this.childExpressions == null) {
      return false;
    }
    if (this.childExpressions.size() != 1) {
      return false;
    }
    return this.childExpressions.get(0).isCount();
  }

  public SimpleNode splitForAggregation(
      AggregateProjectionSplit aggregateProj, CommandContext ctx) {
    var db = ctx.getDatabase();
    if (isAggregate(db)) {
      var result = new SQLMathExpression(-1);
      if (this.childExpressions != null && this.operators != null) {
        var i = 0;
        for (var expr : this.childExpressions) {
          if (i > 0) {
            result.addOperator(operators.get(i - 1));
          }
          var splitResult = expr.splitForAggregation(aggregateProj, ctx);
          if (splitResult instanceof SQLMathExpression res) {
            if (res.isEarlyCalculated(ctx) || res.isAggregate(db)) {
              result.addChildExpression(res);
            } else {
              throw new CommandExecutionException(
                  "Cannot mix aggregate and single record attribute values in the same projection");
            }
          } else if (splitResult instanceof SQLExpression) {
            result.addChildExpression(
                ((SQLExpression) splitResult)
                    .mathExpression); // this comes from a splitted aggregate function
          }
          i++;
        }
      }
      return result;
    } else {
      return this;
    }
  }

  public AggregationContext getAggregationContext(CommandContext ctx) {
    throw new UnsupportedOperationException(
        "multiple math expressions do not allow plain aggregation");
  }

  public SQLMathExpression copy() {
    SQLMathExpression result = null;
    try {
      result = getClass().getConstructor(Integer.TYPE).newInstance(-1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (this.childExpressions != null) {
      result.childExpressions =
          childExpressions.stream().map(x -> x.copy()).collect(Collectors.toList());
    }
    if (operators != null) {
      result.operators = new ArrayList<>(operators);
    }
    return result;
  }

  public void extractSubQueries(SQLIdentifier letAlias, SubQueryCollector collector) {
    if (this.childExpressions != null) {
      for (var expr : this.childExpressions) {
        expr.extractSubQueries(letAlias, collector);
      }
    }
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (this.childExpressions != null) {
      for (var expr : this.childExpressions) {
        expr.extractSubQueries(collector);
      }
    }
  }

  public boolean refersToParent() {
    if (this.childExpressions != null) {
      for (var expr : this.childExpressions) {
        if (expr.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLMathExpression) o;

    if (!Objects.equals(childExpressions, that.childExpressions)) {
      return false;
    }
    return Objects.equals(operators, that.operators);
  }

  @Override
  public int hashCode() {
    var result = childExpressions != null ? childExpressions.hashCode() : 0;
    result = 31 * result + (operators != null ? operators.hashCode() : 0);
    return result;
  }

  public List<String> getMatchPatternInvolvedAliases() {
    List<String> result = new ArrayList<String>();
    if (this.childExpressions != null) {
      for (var exp : childExpressions) {
        var x = exp.getMatchPatternInvolvedAliases();
        if (x != null) {
          result.addAll(x);
        }
      }
    }
    if (result.size() == 0) {
      return null;
    }
    return result;
  }

  public void applyRemove(ResultInternal result, CommandContext ctx) {
    if (childExpressions == null || childExpressions.size() != 1) {
      throw new CommandExecutionException("cannot apply REMOVE " + this);
    }
    childExpressions.get(0).applyRemove(result, ctx);
  }

  public static SQLMathExpression deserializeFromResult(Result fromResult) {
    String className = fromResult.getProperty("__class");
    try {
      var result =
          (SQLMathExpression) Class.forName(className).getConstructor(Integer.class)
              .newInstance(-1);
      result.deserialize(fromResult);
      return result;
    } catch (Exception e) {
      throw BaseException.wrapException(new CommandExecutionException(""), e);
    }
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    result.setProperty("__class", getClass().getName());
    if (childExpressions != null) {
      result.setProperty(
          "childExpressions",
          childExpressions.stream().map(x -> x.serialize(db)).collect(Collectors.toList()));
    }
    if (operators != null) {
      result.setProperty(
          "operators",
          operators.stream().map(this::serializeOperator).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(Result fromResult) {
    if (fromResult.getProperty("childExpressions") != null) {
      List<Result> ser = fromResult.getProperty("childExpressions");
      childExpressions =
          ser.stream().map(x -> deserializeFromResult(x)).collect(Collectors.toList());
    }
    if (fromResult.getProperty("operators") != null) {
      List<String> ser = fromResult.getProperty("operators");
      operators = ser.stream().map(x -> deserializeOperator(x)).collect(Collectors.toList());
    }
  }

  private String serializeOperator(Operator x) {
    return x.toString();
  }

  private Operator deserializeOperator(String x) {
    return Operator.valueOf(x);
  }
}
/* JavaCC - OriginalChecksum=c255bea24e12493e1005ba2a4d1dbb9d (do not edit this line) */
