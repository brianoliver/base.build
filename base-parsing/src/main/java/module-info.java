/*-
 * #%L
 * base.build Parsing
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
 * Provides commonly <i>Parsing</i> types.
 *
 * @author brian.oliver
 * @since Oct-2024
 */
module build.base.parsing {
    requires build.base.foundation;
    requires transitive build.base.io;

    exports build.base.parsing;
    exports build.base.parsing.resolvers;
    exports build.base.parsing.tokenparsers;
}
