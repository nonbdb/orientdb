/*
 *
 *
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *
 *
 */
package com.jetbrains.youtrack.db.internal.core.command.script.formatter;

import com.jetbrains.youtrack.db.internal.core.db.DatabaseSessionInternal;
import com.jetbrains.youtrack.db.internal.core.metadata.function.Function;

/**
 * Javascript script formatter.
 */
public class JSScriptFormatter implements ScriptFormatter {

  public String getFunctionDefinition(DatabaseSessionInternal session, final Function f) {

    final StringBuilder fCode = new StringBuilder(1024);
    fCode.append("function ");
    fCode.append(f.getName(session));
    fCode.append('(');
    int i = 0;
    if (f.getParameters(session) != null) {
      for (String p : f.getParameters(session)) {
        if (i++ > 0) {
          fCode.append(',');
        }
        fCode.append(p);
      }
    }
    fCode.append(") {\n");
    fCode.append(f.getCode(session));
    fCode.append("\n}\n");

    return fCode.toString();
  }

  @Override
  public String getFunctionInvoke(DatabaseSessionInternal session, final Function iFunction,
      final Object[] iArgs) {
    final StringBuilder code = new StringBuilder(1024);

    code.append(iFunction.getName(session));
    code.append('(');
    if (iArgs != null) {
      int i = 0;
      for (Object a : iArgs) {
        if (i++ > 0) {
          code.append(',');
        }
        code.append(a);
      }
    }
    code.append(");");

    return code.toString();
  }
}
