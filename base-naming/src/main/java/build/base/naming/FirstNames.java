package build.base.naming;

/*-
 * #%L
 * base.build Naming
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

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * Provides popular first names.
 *
 * @author brian.oliver
 * @since Aug-2021
 */
public class FirstNames {

    /**
     * Popular first names.
     */
    private static final String[] NAMES = {
        "Aaliyah", "Aaron", "Abel", "Abigail", "Ace", "Ada", "Adalyn", "Adalynn", "Adam", "Addison", "Adeline",
        "Adrian", "Adriel", "Aiden", "Aj", "Alaina", "Alani", "Alex", "Alexander", "Ali", "Alia", "Alice", "Alina",
        "Aliyah", "Allison", "Amara", "Amari", "Amaya", "Amelia", "Amir", "Anand", "Anastasia", "Andrew", "Angel",
        "Anna", "Anthony", "Antonio", "Arabella", "Archer", "Aria", "Ariana", "Arianna", "Ariella", "Arthur", "Arya",
        "Asher", "Ashton", "Aspen", "Athena", "Atlas", "Aubree", "Aubrey", "Audrey", "August", "Aurora", "Austin",
        "Autumn", "Ava", "Avery", "Axel", "Ayden", "Ayla", "Bailey", "Beau", "Bebesh", "Beckett", "Bella", "Ben",
        "Benjamin", "Bennett", "Bentley", "Blakely", "Braxton", "Brayden", "Brian", "Brielle", "Brooklyn", "Brooks",
        "Brynlee", "Bryson", "Caleb", "Callie", "Calvin", "Camden", "Cameron", "Camila", "Caroline", "Carson", "Carter",
        "Catalina", "Cecilia", "Charles", "Charlie", "Charlotte", "Chase", "Chloe", "Chris", "Christian", "Christopher",
        "Claire", "Clara", "Cole", "Colt", "Colton", "Connor", "Cooper", "Cora", "Daisy", "Damian", "Daniel", "David",
        "Dawson", "Dean", "Declan", "Delilah", "Diego", "Dominic", "Dylan", "Easton", "Eden", "Edward", "Eleanor",
        "Elena", "Eli", "Eliana", "Elias", "Elijah", "Elisa", "Eliza", "Elizabeth", "Ella", "Elliana", "Ellie",
        "Elliot", "Elliott", "Eloise", "Ember", "Emersyn", "Emery", "Emilia", "Emily", "Emma", "Emmett", "Enzo",
        "Ethan", "Eva", "Evan", "Evelyn", "Everett", "Everleigh", "Everly", "Evie", "Ezekiel", "Ezra", "Faith", "Finn",
        "Freya", "Gabriel", "Gabriella", "Gael", "Gautam", "Gavin", "Genesis", "Genevieve", "George", "Gianna",
        "Giovanni", "Grace", "Gracie", "Graeme", "Grayson", "Greyson", "Hadley", "Hailey", "Hannah", "Harmony",
        "Harper", "Harrison", "Hayden", "Hazel", "Henry", "Hudson", "Hueseyin", "Hunter", "Ian", "Imogen", "Iris",
        "Isaac", "Isabel", "Isabella", "Isabelle", "Isaiah", "Isla", "Ivan", "Ivy", "Jace", "Jack", "Jackson", "Jacky",
        "Jacob", "Jade", "Jake", "James", "Jameson", "Jasmine", "Jason", "Jasper", "Jax", "Jaxon", "Jaxson", "Jayce",
        "Jayden", "Jeremiah", "Jeriah", "Jerry", "Jesse", "John", "Jonah", "Jonathan", "Jordan", "Jordyn", "Joseph",
        "Josephine", "Joshua", "Josiah", "Josie", "Journee", "Jude", "Julia", "Julian", "Juniper", "Kai", "Kaiden",
        "Kanan", "Karter", "Kashif", "Kavitha", "Kayden", "Kaylee", "Kehlani", "Kennedy", "Khloe", "King", "Kingston",
        "Kinsley", "Kylie", "Kyrie", "Laila", "Landon", "Layla", "Leah", "Legend", "Leila", "Leilani", "Leo", "Leon",
        "Leonardo", "Leriel", "Levi", "Liam", "Liliana", "Lillian", "Lilly", "Lily", "Lina", "Lincoln", "Litte",
        "Logan", "Londyn", "Lorenzo", "Luca", "Lucas", "Lucia", "Lucy", "Luka", "Lukas", "Luke", "Luna", "Lydia",
        "Lyla", "Mackenzie", "Maddox", "Madeline", "Madelyn", "Madison", "Maeve", "Magana", "Maggie", "Magnolia",
        "Malachi", "Maria", "Mark", "Maryam", "Mason", "Mateo", "Matteo", "Matthew", "Maverick", "Max", "Maya",
        "Melanie", "Melody", "Messiah", "Mia", "Micah", "Michael", "Micheal", "Michelle", "Miguel", "Mila", "Miles",
        "Millie", "Milo", "Mohammed", "Muhammad", "Myles", "Naomi", "Natalia", "Natalie", "Nathan", "Nathaniel",
        "Nevaeh", "Nicholas", "Noah", "Nolan", "Nora", "Norah", "Nova", "Nur", "Nyla", "Oakley", "Oaklynn", "Octavia",
        "Oliver", "Olivia", "Omar", "Owen", "Paisley", "Parker", "Paulo", "Penelope", "Peter", "Peyton", "Piper",
        "Prince", "Princess", "Quinn", "Raelynn", "Reagan", "Reed", "Reese", "Remi", "Rhett", "Riley", "River", "Rob",
        "Robert", "Roman", "Rose", "Rowan", "Ruby", "Ryan", "Ryder", "Ryilee", "Ryker", "Rylee", "Ryleigh", "Sadie",
        "Sage", "Salva", "Samantha", "Samuel", "Santiago", "Sara", "Sarah", "Savannah", "Sawyer", "Scarlett", "Sean",
        "Sebastian", "Selena", "Selina", "Serenity", "Sienna", "Silas", "Skylar", "Sloane", "Sofia", "Sophia", "Sophie",
        "Spencer", "Stella", "Stephanie", "Summer", "Talia", "Terry", "Theo", "Theodore", "Thomas", "Travis", "Tristan",
        "Tucker", "Tyler", "Unnati", "Valentina", "Valerie", "Victoria", "Vincent", "Violet", "Vivian", "Walker",
        "Waylon", "Wesley", "Weston", "William", "Willow", "Wyatt", "Xander", "Xavier", "Ximena", "Zachary", "Zane",
        "Zara", "Zayden", "Zion", "Zoe", "Zoey", "Zuri"
    };

    /**
     * Private constructor to prevent construction.
     */
    private FirstNames() {
    }

    /**
     * Obtains a {@link Stream} of the first names.
     *
     * @return a {@link Stream} of first names
     */
    public static Stream<String> stream() {
        return Arrays.stream(NAMES);
    }

    /**
     * Obtains a random first name.
     *
     * @return a random first name
     */
    public static String getRandom() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return get(random.nextInt(NAMES.length));
    }

    /**
     * Obtains the nth first name, zero being the first.
     * <p>
     * Should the provided index be outside the bounds of the number of first names, the mod of the positive index will
     * be used to produce a first name, thus always guaranteeing a first name will be produced.
     *
     * @param nth the adjective to obtain
     * @return the nth adjective
     */
    public static String get(final int nth) {
        return NAMES[Math.abs(nth) % NAMES.length];
    }

    /**
     * Determines the number of first names.
     *
     * @return the number of first names
     */
    public static int count() {
        return NAMES.length;
    }
}
