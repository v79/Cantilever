package org.liamjd.cantilever.common

object s3Keys {
    const val sourcesKey = "sources/"
    const val fragmentsKey = "generated/htmlFragments/"
    const val templatesKey = "templates/"
}

object fileTypes {
    const val HTML_HBS = ".html.hbs"
}

/*

CLI:
s3://cantileverstack-cantileversources123976ca-u9xp6zshycpg/templates/post.hbs

WEB:
s3://cantileverstack-cantileversources123976ca-u9xp6zshycpg/templates/post.hbs

JDK:
     cantileverstack-cantileversources123976ca-u9xp6zshycpg templates/post.hbs


*/
