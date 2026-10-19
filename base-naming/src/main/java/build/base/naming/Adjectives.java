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
 * Provides popular adjectives.
 *
 * @author brian.oliver
 * @since Aug-2021
 */
public class Adjectives {

    private static final String[] ADJECTIVES = {
        "Accepting", "Adventurous", "Affectionate", "Afraid", "Aggravated", "Agitated", "Aloof", "Amazed", "Ambitious",
        "Anguished", "Anxious", "Appreciative", "Apprehensive", "Ashamed", "Attentive", "Bitter", "Blessed", "Blissful",
        "Bored", "Bossy", "Brave", "Calm", "Capable", "Careful", "Careless", "Caring", "Centered", "Cheeky", "Cheerful",
        "Compassionate", "Concerned", "Confident", "Confused", "Conscientious", "Contented", "Courageous", "Courteous",
        "Cranky", "Crazy", "Creative", "Cynical", "Daring", "Delighted", "Depleted", "Depressed", "Despondent",
        "Determined", "Diplomatic", "Disappointed", "Discouraged", "Discreet", "Disdainful", "Disgruntled", "Dishonest",
        "Disobedient", "Dissatisfied", "Distant", "Distracted", "Disturbed", "Dynamic", "Eager", "Easygoing",
        "Ecstatic", "Edgy", "Emotional", "Empathetic", "Empty", "Enchanted", "Encouraged", "Energetic", "Energized",
        "Engaged", "Enthusiastic", "Exasperated", "Excited", "Exhausted", "Expectant", "Exploring", "Faithful",
        "Fascinated", "Fearless", "Forlorn", "Fortunate", "Frank", "Frazzled", "Free", "Friendly", "Frightened",
        "Frustrated", "Fulfilled", "Funny", "Furious", "Generous", "Gentle", "Gloomy", "Graceful", "Grateful",
        "Grouchy", "Grounded", "Happy", "Hardworking", "Heartbroken", "Helpful", "Helpless", "Hesitant", "Honest",
        "Hopeless", "Hostile", "Humble", "Humbled", "Humiliated", "Humorous", "Hypocritical", "Imaginative",
        "Impartial", "Impatient", "Indifferent", "Inhibited", "Inspired", "Intellectual", "Intelligent", "Interested",
        "Intrigued", "Invigorated", "Involved", "Irate", "Irritated", "Isolated", "Jealous", "Kind", "Lazy",
        "Lethargic", "Listless", "Lively", "Lonely", "Longing", "Loving", "Loyal", "Lucky", "Mean", "Melancholy",
        "Messy", "Modest", "Moody", "Mortified", "Moved", "Naughty", "Neat", "Nervous", "Nice", "Obedient",
        "Optimistic", "Outraged", "Overwhelmed", "Panicking", "Paralyzed", "Passionate", "Patient", "Peaceful",
        "Perplexed", "Persistent", "Pessimistic", "Placid", "Playful", "Plucky", "Polite", "Popular", "Present",
        "Proud", "Questioning", "Radiant", "Rational", "Rattled", "Reflective", "Refreshed", "Regretful", "Rejecting",
        "Rejuvenated", "Relaxed", "Reliable", "Reluctant", "Remorseful", "Removed", "Renewed", "Resentful", "Reserved",
        "Resistant", "Restless", "Romantic", "Rude", "Safe", "Satisfied", "Scared", "Selfish", "Sensible", "Sensitive",
        "Serene", "Serious", "Shaken", "Shocked", "Shy", "Silly", "Sincere", "Skeptical", "Smart", "Sociable",
        "Sorrowful", "Stimulated", "Straightforward", "Strong", "Stubborn", "Supportive", "Suspicious", "Talkative",
        "Teary", "Terrified", "Thankful", "Thoughtful", "Thrilled", "Tidy", "Tight", "Timid", "Touched", "Trusting",
        "Uneasy", "Unfriendly", "Ungrounded", "Unhappy", "Unpleasant", "Unsure", "Untidy", "Upset", "Valiant",
        "Versatile", "Vibrant", "Vindictive", "Vulnerable", "Warm", "Weary", "Withdrawn", "Worried", "Worthy",
        "Yearning"
    };

    /**
     * Private constructor to prevent construction.
     */
    private Adjectives() {
    }

    /**
     * Obtains the {@link Stream} of adjectives.
     *
     * @return the {@link Stream} of adjectives
     */
    public static Stream<String> stream() {
        return Arrays.stream(ADJECTIVES);
    }

    /**
     * Obtains a random adjective.
     *
     * @return a random adjective
     */
    public static String getRandom() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return get(random.nextInt(ADJECTIVES.length));
    }

    /**
     * Obtains the nth adjective, zero being the first.
     * <p>
     * Should the provided index be outside the bounds of the number of adjectives, the mod of the positive index will
     * be used to produce an adjective, always guaranteeing an adjective will be produced.
     *
     * @param nth the adjective to obtain
     * @return the nth adjective
     */
    public static String get(final int nth) {
        return ADJECTIVES[Math.abs(nth) % ADJECTIVES.length];
    }

    /**
     * Determines the number of adjectives.
     *
     * @return the number of adjectives
     */
    public static int count() {
        return ADJECTIVES.length;
    }
}
