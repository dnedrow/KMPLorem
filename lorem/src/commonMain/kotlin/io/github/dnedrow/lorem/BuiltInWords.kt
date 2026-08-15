package io.github.dnedrow.lorem

/**
 * The 210 Latin words shipped with the library, in their canonical order.
 *
 * The list is compiled into the common source set rather than loaded from a resource bundle so
 * that every target — including those with no bundle or file-system concept — resolves it
 * identically and without failure. See `design.md` decision D3.
 */
internal val BUILT_IN_WORDS: List<String> = listOf(
    "a", "ab", "accusamus", "accusantium", "ad", "adipisci",
    "adipiscing", "alias", "aliqua", "aliquam", "aliquid", "aliquip",
    "amet", "anim", "animi", "aperiam", "architecto", "asperiores",
    "aspernatur", "assumenda", "at", "atque", "aut", "aute",
    "autem", "beatae", "blanditiis", "cillum", "commodi", "commodo",
    "consectetur", "consequat", "consequatur", "consequuntur", "corporis", "corrupti",
    "culpa", "cum", "cumque", "cupidatat", "cupiditate", "debitis",
    "delectus", "deleniti", "deserunt", "dicta", "dignissimos", "distinctio",
    "do", "dolor", "dolore", "dolorem", "doloremque", "dolores",
    "doloribus", "dolorum", "ducimus", "duis", "ea", "eaque",
    "earum", "eius", "eiusmod", "eligendi", "elit", "enim",
    "eos", "error", "esse", "est", "et", "eu",
    "eum", "eveniet", "ex", "excepteur", "excepturi", "exercitation",
    "exercitationem", "expedita", "explicabo", "facere", "facilis", "fuga",
    "fugiat", "fugit", "harum", "hic", "id", "illo",
    "illum", "impedit", "in", "incididunt", "incidunt", "inventore",
    "ipsa", "ipsam", "ipsum", "irure", "iste", "itaque",
    "iure", "iusto", "labore", "laboriosam", "laboris", "laborum",
    "laudantium", "libero", "lorem", "magna", "magnam", "magni",
    "maiores", "maxime", "minim", "minima", "minus", "modi",
    "molestiae", "molestias", "mollit", "mollitia", "nam", "natus",
    "necessitatibus", "nemo", "neque", "nesciunt", "nihil", "nisi",
    "nobis", "non", "nostrud", "nostrum", "nulla", "numquam",
    "occaecat", "occaecati", "odio", "odit", "officia", "officiis",
    "omnis", "optio", "pariatur", "perferendis", "perspiciatis", "placeat",
    "porro", "possimus", "praesentium", "proident", "provident", "quae",
    "quaerat", "quam", "quas", "quasi", "qui", "quia",
    "quibusdam", "quidem", "quis", "quisquam", "quo", "quod",
    "quos", "ratione", "recusandae", "reiciendis", "rem", "repellat",
    "repellendus", "reprehenderit", "repudiandae", "rerum", "saepe", "sapiente",
    "sed", "sequi", "similique", "sint", "sit", "soluta",
    "sunt", "suscipit", "tempor", "tempora", "tempore", "temporibus",
    "tenetur", "totam", "ullam", "ullamco", "unde", "ut",
    "vel", "velit", "veniam", "veritatis", "vero", "vitae",
    "voluptas", "voluptate", "voluptatem", "voluptates", "voluptatibus", "voluptatum",
)
