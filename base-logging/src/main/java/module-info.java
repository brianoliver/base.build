/*-
 * #%L
 * base.build Logging
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
 * A simple {@code java.util.logging} (JUL) {@code Logger} adapter for more pleasant and advanced logging capabilities.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
module build.base.logging {
    requires java.logging;
    requires build.base.flow;
    requires build.base.foundation;

    exports build.base.logging;
}
