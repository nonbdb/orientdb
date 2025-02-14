/**
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*
 */
package com.cloudbees.syslog;

/**
 * Format of the Syslog message.
 */
public enum MessageFormat {
  /**
   * <a href="http://tools.ietf.org/html/rfc3164">RFC 3614 - BSD syslog Protocol</a>
   */
  RFC_3164,
  /**
   * <a href="https://tools.ietf.org/html/rfc5424">RFC 5424 - The Syslog Protocol</a>
   */
  RFC_5424
}
