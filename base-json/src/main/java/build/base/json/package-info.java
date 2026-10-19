/*-
 * #%L
 * base.build JSON
 * %%
 * Copyright (C) 2026 The base.build Authors and Contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * This package tracks the API proposed in JEP draft 8344154 ({@code java.util.json}). The draft is not yet final;
 * the shape of {@link build.base.json.JsonValue} and its subtypes may change before the JDK ships a standard JSON
 * API. On arrival, this module will be deprecated in favor of {@code java.util.json} and consumers will migrate by
 * updating imports.
 */
package build.base.json;
