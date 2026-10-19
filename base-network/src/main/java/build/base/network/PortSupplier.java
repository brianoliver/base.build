package build.base.network;

/*-
 * #%L
 * base.build Network
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

import java.util.function.IntSupplier;

/**
 * A {@link IntSupplier} which supplies {@link Integer}s representing valid, unused networking ports.
 *
 * @author graeme.campbell
 * @see <a href="https://en.wikipedia.org/wiki/Port_(computer_networking)">Port (computer networking)</a>
 * @since Sep-2019
 */
public interface PortSupplier
    extends IntSupplier {

}
