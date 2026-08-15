package io.github.dnedrow.lorem

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Fixed-seed golden tests pinning exact output.
 *
 * These strings are stable because [SeededRandom] owns its SplitMix64 core rather than delegating
 * to `kotlin.random.Random`, whose sequence is only guaranteed within one Kotlin runtime version.
 * A change here means generation behavior changed — review it before updating the expectation.
 */
class GoldenTest {

    private val dictionary = LoremDictionary.BuiltIn

    @Test
    fun sentenceGolden() {
        assertEquals(
            "Quamvis magna adipisci quisquam asperiores aut, proident asperiores doloribus " +
                "deleniti ullamco labore minima aliqua.",
            LoremGenerator(dictionary, 42L).generateSentence(),
        )
    }

    @Test
    fun classicParagraphGolden() {
        assertEquals(
            "Quamvis adipisci quisquam asperiores aut, proident asperiores doloribus deleniti " +
                "ullamco labore minima. Autem, cupidatat officiis animi aperiam doloremque " +
                "ratione eaque, et unde ullam tempor aliquam tempor debitis laborum. Cumque ea " +
                "minus iusto id nobis inventore ducimus laudantium corrupti irure. Eum " +
                "voluptatem explicabo, et doloribus repellendus odit incidunt. Ea alias totam et " +
                "aliquid ratione consectetur voluptas corrupti.",
            LoremGenerator(dictionary, 42L).generateParagraph(ParagraphStyle.Classic),
        )
    }

    @Test
    fun technicalParagraphGolden() {
        assertEquals(
            "Adipisci quisquam asperiores aut proident: asperiores doloribus, deleniti ullamco, " +
                "et labore minima. Autem, cupidatat officiis animi aperiam doloremque ratione " +
                "eaque, et unde ullam tempor aliquam tempor debitis laborum. Insuper, quamvis ea " +
                "minus iusto id, nobis inventore ducimus laudantium corrupti. Dicta eum " +
                "voluptatem explicabo doloribus repellendus odit incidunt duis sint. " +
                "Consequenter, et aliquid ratione consectetur voluptas: corrupti cupidatat, amet " +
                "minus, et quo quae. Quamvis autem nulla quos ratione anim voluptas at, repellat " +
                "deleniti ullam itaque provident quis voluptatem asperiores repellendus omnis " +
                "architecto.",
            LoremGenerator(dictionary, 42L).generateParagraph(ParagraphStyle.Technical),
        )
    }

    @Test
    fun academicParagraphGolden() {
        assertEquals(
            "Quamvis adipisci quisquam asperiores aut, proident asperiores doloribus deleniti " +
                "ullamco labore minima. Autem, cupidatat officiis animi aperiam doloremque: " +
                "ratione eaque unde, ullam tempor aliquam, et tempor debitis laborum. Insuper, " +
                "ea minus iusto id, et nobis inventore ducimus laudantium corrupti. Dicta eum " +
                "voluptatem explicabo doloribus repellendus odit incidunt duis sint. " +
                "Consequenter, et aliquid ratione consectetur voluptas, et corrupti cupidatat " +
                "amet minus quo quae.",
            LoremGenerator(dictionary, 42L).generateParagraph(ParagraphStyle.Academic),
        )
    }

    @Test
    fun legalParagraphGolden() {
        assertEquals(
            "Quamvis adipisci quisquam asperiores aut, proident asperiores doloribus deleniti " +
                "ullamco labore minima. Autem, cupidatat officiis animi aperiam doloremque " +
                "ratione eaque, et unde ullam tempor aliquam tempor debitis laborum. Cumque ea " +
                "minus iusto id nobis inventore ducimus laudantium corrupti irure.",
            LoremGenerator(dictionary, 42L).generateParagraph(ParagraphStyle.Legal),
        )
    }

    @Test
    fun mixedParagraphGolden() {
        assertEquals(
            "Adipisci quisquam asperiores aut proident asperiores doloribus deleniti ullamco " +
                "labore minima aliqua. Cupidatat: officiis animi, aperiam doloremque, et ratione " +
                "eaque. Tempor debitis laborum eius adipisci nulla omnis cumque ea minus iusto " +
                "id nobis inventore ducimus laudantium corrupti irure expedita in. Autem, " +
                "quamvis doloribus repellendus, odit incidunt. Ea alias totam et, et aliquid " +
                "ratione consectetur voluptas.",
            LoremGenerator(dictionary, 42L).generateParagraph(ParagraphStyle.Mixed),
        )
    }

    @Test
    fun batchGolden() {
        assertEquals(
            listOf(
                "Rem quia reprehenderit autem officia itaque inventore officiis? Quamvis sequi " +
                    "praesentium id, voluptates sint doloribus deleniti. Corporis commodi cillum " +
                    "a sint: aperiam error odit corrupti! Quamvis voluptates ullam, cillum iure " +
                    "incididunt voluptate.",
                "Iure incidunt nobis debitis excepteur ducimus dolores quia et doloremque " +
                    "dolores soluta dolore irure temporibus? Quamvis odio exercitationem sequi " +
                    "repellat aspernatur consequatur nesciunt, pariatur quas lorem rem commodo " +
                    "cupidatat minima est fuga eius. Fugit quibusdam, et facilis harum quia. " +
                    "Neque cum nihil consectetur laudantium nesciunt.",
                "Placeat esse alias veniam necessitatibus earum corrupti hic? Quamvis excepteur " +
                    "praesentium corrupti id nulla id ex, dignissimos quam ducimus nulla " +
                    "adipisci dolorem voluptate ratione commodi sequi. Libero anim ea ut quia " +
                    "tempor, et vero quas incidunt quae laboriosam corrupti occaecat. Molestiae " +
                    "a similique accusantium quisquam tempor non nihil ab cupidatat dolorem " +
                    "numquam laboriosam dolor assumenda dicta.",
            ),
            LoremGenerator(dictionary, 7L).generateParagraphs(3),
        )
    }

    @Test
    fun canonicalOpeningParagraphGolden() {
        val generator = LoremGenerator(
            dictionary,
            42L,
            GeneratorConfiguration(startsWithCanonicalOpening = true),
        )
        assertEquals(
            "Lorem ipsum dolor sit amet adipisci quisquam asperiores aut proident asperiores " +
                "doloribus. Aliqua et: quod, aperiam, et praesentium. Insuper, ratione eaque " +
                "unde ullam tempor aliquam tempor debitis laborum eius adipisci nulla omnis? " +
                "Consequenter, nobis inventore ducimus laudantium corrupti: irure expedita in " +
                "ab, dicta eum voluptatem explicabo, et doloribus repellendus odit incidunt. Ea " +
                "alias totam et aliquid ratione consectetur voluptas corrupti.",
            generator.generateParagraph(),
        )
    }
}
