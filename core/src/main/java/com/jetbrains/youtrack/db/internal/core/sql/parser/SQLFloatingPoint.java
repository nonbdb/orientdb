/* Generated By:JJTree: Do not edit this line. SQLFloatingPoint.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.jetbrains.youtrack.db.internal.core.sql.parser;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.api.query.Result;
import com.jetbrains.youtrack.db.internal.core.sql.executor.ResultInternal;
import java.util.Map;
import java.util.Objects;

public class SQLFloatingPoint extends SQLNumber {

  protected int sign = 1;
  protected String stringValue = null;
  Number finalValue = null;

  public SQLFloatingPoint(int id) {
    super(id);
  }

  public SQLFloatingPoint(YouTrackDBSql p, int id) {
    super(p, id);
  }

  @Override
  public Number getValue() {
    if (finalValue != null) {
      return finalValue;
    }
    if (stringValue.endsWith("F") || stringValue.endsWith("f")) {
      try {
        finalValue = Float.parseFloat(stringValue.substring(0, stringValue.length() - 1)) * sign;
      } catch (Exception ignore) {
        return null; // TODO NaN?
      }
    } else if (stringValue.endsWith("D") || stringValue.endsWith("d")) {
      try {
        finalValue = Double.parseDouble(stringValue.substring(0, stringValue.length() - 1)) * sign;
      } catch (Exception ignore) {
        return null; // TODO NaN?
      }
    } else {
      try {
        var returnValue = Double.parseDouble(stringValue) * sign;
        if (Math.abs(returnValue) < Float.MAX_VALUE) {
          finalValue = (float) returnValue;
        } else {
          finalValue = returnValue;
        }
      } catch (Exception ignore) {
        return null; // TODO NaN?
      }
    }
    return finalValue;
  }

  public int getSign() {
    return sign;
  }

  public void setSign(int sign) {
    this.sign = sign;
  }

  public String getStringValue() {
    return stringValue;
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (sign == -1) {
      builder.append("-");
    }
    builder.append(stringValue);
  }

  @Override
  public SQLFloatingPoint copy() {
    var result = new SQLFloatingPoint(-1);
    result.sign = sign;
    result.stringValue = stringValue;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    var that = (SQLFloatingPoint) o;

    if (sign != that.sign) {
      return false;
    }
    return Objects.equals(stringValue, that.stringValue);
  }

  @Override
  public int hashCode() {
    var result = sign;
    result = 31 * result + (stringValue != null ? stringValue.hashCode() : 0);
    return result;
  }

  public Result serialize(DatabaseSessionInternal db) {
    var result = new ResultInternal(db);
    result.setProperty("sign", sign);
    result.setProperty("stringValue", stringValue);
    result.setProperty("finalValue", finalValue);
    return result;
  }

  public void deserialize(Result fromResult) {
    sign = fromResult.getProperty("sign");
    stringValue = fromResult.getProperty("stringValue");
    finalValue = fromResult.getProperty("finalValue");
  }
}
/* JavaCC - OriginalChecksum=46acfb589f666717595e28f1b19611ae (do not edit this line) */
