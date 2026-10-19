package build.base.marshalling.tutorial;

/*-
 * #%L
 * base.build Marshalling
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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A simple marshallable person with a favorite {@link Color}.
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public class PersonWithFavoriteColor {

    private String firstName;
    private String lastName;
    private Color favoriteColor;

    /**
     * Constructs a {@link PersonWithFavoriteColor}.
     *
     * @param firstName     the first name
     * @param lastName      the last name
     * @param favoriteColor the favorite {@link Color}
     */
    @Unmarshal
    public PersonWithFavoriteColor(final String firstName,
                                   final String lastName,
                                   final Color favoriteColor) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.favoriteColor = favoriteColor;
    }

    /**
     * Destructs a {@link PersonWithFavoriteColor}
     *
     * @param firstName the first name
     * @param lastName  the last name
     */
    @Marshal
    public void destructor(final Out<String> firstName,
                           final Out<String> lastName,
                           final Out<Color> favoriteColor) {

        firstName.set(this.firstName);
        lastName.set(this.lastName);
        favoriteColor.set(this.favoriteColor);
    }

    public String firstName() {
        return this.firstName;
    }

    public String lastName() {
        return this.lastName;
    }

    public Color favoriteColor() {
        return this.favoriteColor;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final PersonWithFavoriteColor that)) {
            return false;
        }
        return Objects.equals(this.firstName, that.firstName)
            && Objects.equals(this.lastName, that.lastName)
            && Objects.equals(this.favoriteColor, that.favoriteColor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.firstName, this.lastName, this.favoriteColor);
    }

    static {
        // self-register this type when it's initialized
        Marshalling.register(PersonWithFavoriteColor.class, MethodHandles.lookup());
    }
}
