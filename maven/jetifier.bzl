load("@bazel_tools//tools/build_defs/repo:http.bzl", _http_archive = "http_archive")
load(":sets.bzl", "sets")

# Based on https://developer.android.com/jetpack/androidx/migrate/artifact-mappings
JETIFIER_ARTIFACT_MAPPING = {
    "android.arch.core:common": "androidx.arch.core:core-common",
    "android.arch.core:core": "androidx.arch.core:core",
    "android.arch.core:core-testing": "androidx.arch.core:core-testing",
    "android.arch.core:runtime": "androidx.arch.core:core-runtime",
    "android.arch.lifecycle:common": "androidx.lifecycle:lifecycle-common",
    "android.arch.lifecycle:common-java8": "androidx.lifecycle:lifecycle-common-java8",
    "android.arch.lifecycle:compiler": "androidx.lifecycle:lifecycle-compiler",
    "android.arch.lifecycle:extensions": "androidx.lifecycle:lifecycle-extensions",
    "android.arch.lifecycle:livedata": "androidx.lifecycle:lifecycle-livedata",
    "android.arch.lifecycle:livedata-core": "androidx.lifecycle:lifecycle-livedata-core",
    "android.arch.lifecycle:reactivestreams": "androidx.lifecycle:lifecycle-reactivestreams",
    "android.arch.lifecycle:runtime": "androidx.lifecycle:lifecycle-runtime",
    "android.arch.lifecycle:viewmodel": "androidx.lifecycle:lifecycle-viewmodel",
    "android.arch.paging:common": "androidx.paging:paging-common",
    "android.arch.paging:runtime": "androidx.paging:paging-runtime",
    "android.arch.paging:rxjava2": "androidx.paging:paging-rxjava2",
    "android.arch.persistence.room:common": "androidx.room:room-common",
    "android.arch.persistence.room:compiler": "androidx.room:room-compiler",
    "android.arch.persistence.room:guava": "androidx.room:room-guava",
    "android.arch.persistence.room:migration": "androidx.room:room-migration",
    "android.arch.persistence.room:runtime": "androidx.room:room-runtime",
    "android.arch.persistence.room:rxjava2": "androidx.room:room-rxjava2",
    "android.arch.persistence.room:testing": "androidx.room:room-testing",
    "android.arch.persistence:db": "androidx.sqlite:sqlite",
    "android.arch.persistence:db-framework": "androidx.sqlite:sqlite-framework",
    "com.android.support.constraint:constraint-layout": "androidx.constraintlayout:constraintlayout",
    "com.android.support.constraint:constraint-layout-solver": "androidx.constraintlayout:constraintlayout-solver",
    "com.android.support.test.espresso.idling:idling-concurrent": "androidx.test.espresso.idling:idling-concurrent",
    "com.android.support.test.espresso.idling:idling-net": "androidx.test.espresso.idling:idling-net",
    "com.android.support.test.espresso:espresso-accessibility": "androidx.test.espresso:espresso-accessibility",
    "com.android.support.test.espresso:espresso-contrib": "androidx.test.espresso:espresso-contrib",
    "com.android.support.test.espresso:espresso-core": "androidx.test.espresso:espresso-core",
    "com.android.support.test.espresso:espresso-idling-resource": "androidx.test.espresso:espresso-idling-resource",
    "com.android.support.test.espresso:espresso-intents": "androidx.test.espresso:espresso-intents",
    "com.android.support.test.espresso:espresso-remote": "androidx.test.espresso:espresso-remote",
    "com.android.support.test.espresso:espresso-web": "androidx.test.espresso:espresso-web",
    "com.android.support.test.janktesthelper:janktesthelper": "androidx.test.jank:janktesthelper",
    "com.android.support.test.services:test-services": "androidx.test:test-services",
    "com.android.support.test.uiautomator:uiautomator": "androidx.test.uiautomator:uiautomator",
    "com.android.support.test:monitor": "androidx.test:monitor",
    "com.android.support.test:orchestrator": "androidx.test:orchestrator",
    "com.android.support.test:rules": "androidx.test:rules",
    "com.android.support.test:runner": "androidx.test:runner",
    "com.android.support:animated-vector-drawable": "androidx.vectordrawable:vectordrawable-animated",
    "com.android.support:appcompat-v7": "androidx.appcompat:appcompat",
    "com.android.support:asynclayoutinflater": "androidx.asynclayoutinflater:asynclayoutinflater",
    "com.android.support:car": "androidx.car:car",
    "com.android.support:cardview-v7": "androidx.cardview:cardview",
    "com.android.support:collections": "androidx.collection:collection",
    "com.android.support:coordinatorlayout": "androidx.coordinatorlayout:coordinatorlayout",
    "com.android.support:cursoradapter": "androidx.cursoradapter:cursoradapter",
    "com.android.support:customtabs": "androidx.browser:browser",
    "com.android.support:customview": "androidx.customview:customview",
    "com.android.support:design": "com.google.android.material:material",
    "com.android.support:documentfile": "androidx.documentfile:documentfile",
    "com.android.support:drawerlayout": "androidx.drawerlayout:drawerlayout",
    "com.android.support:exifinterface": "androidx.exifinterface:exifinterface",
    "com.android.support:gridlayout-v7": "androidx.gridlayout:gridlayout",
    "com.android.support:heifwriter": "androidx.heifwriter:heifwriter",
    "com.android.support:interpolator": "androidx.interpolator:interpolator",
    "com.android.support:leanback-v17": "androidx.leanback:leanback",
    "com.android.support:loader": "androidx.loader:loader",
    "com.android.support:localbroadcastmanager": "androidx.localbroadcastmanager:localbroadcastmanager",
    "com.android.support:media2": "androidx.media2:media2",
    "com.android.support:media2-exoplayer": "androidx.media2:media2-exoplayer",
    "com.android.support:mediarouter-v7": "androidx.mediarouter:mediarouter",
    "com.android.support:multidex": "androidx.multidex:multidex",
    "com.android.support:multidex-instrumentation": "androidx.multidex:multidex-instrumentation",
    "com.android.support:palette-v7": "androidx.palette:palette",
    "com.android.support:percent": "androidx.percentlayout:percentlayout",
    "com.android.support:preference-leanback-v17": "androidx.leanback:leanback-preference",
    "com.android.support:preference-v14": "androidx.legacy:legacy-preference-v14",
    "com.android.support:preference-v7": "androidx.preference:preference",
    "com.android.support:print": "androidx.print:print",
    "com.android.support:recommendation": "androidx.recommendation:recommendation",
    "com.android.support:recyclerview-selection": "androidx.recyclerview:recyclerview-selection",
    "com.android.support:recyclerview-v7": "androidx.recyclerview:recyclerview",
    "com.android.support:slices-builders": "androidx.slice:slice-builders",
    "com.android.support:slices-core": "androidx.slice:slice-core",
    "com.android.support:slices-view": "androidx.slice:slice-view",
    "com.android.support:slidingpanelayout": "androidx.slidingpanelayout:slidingpanelayout",
    "com.android.support:support-annotations": "androidx.annotation:annotation",
    "com.android.support:support-compat": "androidx.core:core",
    "com.android.support:support-content": "androidx.contentpager:contentpager",
    "com.android.support:support-core-ui": "androidx.legacy:legacy-support-core-ui",
    "com.android.support:support-core-utils": "androidx.legacy:legacy-support-core-utils",
    "com.android.support:support-dynamic-animation": "androidx.dynamicanimation:dynamicanimation",
    "com.android.support:support-emoji": "androidx.emoji:emoji",
    "com.android.support:support-emoji-appcompat": "androidx.emoji:emoji-appcompat",
    "com.android.support:support-emoji-bundled": "androidx.emoji:emoji-bundled",
    "com.android.support:support-fragment": "androidx.fragment:fragment",
    "com.android.support:support-media-compat": "androidx.media:media",
    "com.android.support:support-tv-provider": "androidx.tvprovider:tvprovider",
    "com.android.support:support-v13": "androidx.legacy:legacy-support-v13",
    "com.android.support:support-v4": "androidx.legacy:legacy-support-v4",
    "com.android.support:support-vector-drawable": "androidx.vectordrawable:vectordrawable",
    "com.android.support:swiperefreshlayout": "androidx.swiperefreshlayout:swiperefreshlayout",
    "com.android.support:textclassifier": "androidx.textclassifier:textclassifier",
    "com.android.support:transition": "androidx.transition:transition",
    "com.android.support:versionedparcelable": "androidx.versionedparcelable:versionedparcelable",
    "com.android.support:viewpager": "androidx.viewpager:viewpager",
    "com.android.support:wear": "androidx.wear:wear",
    "com.android.support:webkit": "androidx.webkit:webkit",
}

BUILD_FILE_CONTENT = """
java_import(
    name = "jetifier_standalone_jars",
    jars = glob(["lib/*.jar"]),
)
java_binary(
    main_class = "com.android.tools.build.jetifier.standalone.Main",
    name = "jetifier_standalone",
    runtime_deps = [
        ":jetifier_standalone_jars"
    ],
    visibility = ["//visibility:public"],
)
"""

def jetifier_init():
    _http_archive(
        sha256 = "8ef877e8245f8dcf8f379b2cdc4958ba714147eb8d559d8334a1840e137e5a2c",
        strip_prefix = "jetifier-standalone",
        name = "bazel_maven_repository_jetifier",
        url = "https://dl.google.com/dl/android/studio/jetifier-zips/1.0.0-beta08/jetifier-standalone.zip",
        build_file_content = BUILD_FILE_CONTENT,
    )

# _jetify_impl and jetify are based on https://github.com/bazelbuild/tools_android/pull/5

def _jetify_impl(ctx):
    srcs = ctx.attr.srcs
    outfiles = []
    for src in srcs:
        for artifact in src.files.to_list():
            jetified_outfile = ctx.actions.declare_file(ctx.attr.name + "_jetified_" + artifact.basename)
            jetify_args = ctx.actions.args()
            jetify_args.add_all(["-l", "error"])
            jetify_args.add_all(["-o", jetified_outfile])
            jetify_args.add_all(["-i", artifact])
            ctx.actions.run(
                mnemonic = "Jetify",
                inputs = [artifact],
                outputs = [jetified_outfile],
                progress_message = "Jetifying {} to create {}.".format(artifact.path, jetified_outfile.path),
                executable = ctx.executable._jetifier,
                arguments = [jetify_args],
                use_default_shell_env = True,
            )
            outfiles.append(jetified_outfile)

    return [DefaultInfo(files = depset(outfiles))]

jetify = rule(
    attrs = {
        "srcs": attr.label_list(allow_files = [".jar", ".aar"]),
        "_jetifier": attr.label(
            executable = True,
            allow_files = True,
            default = Label("@bazel_maven_repository_jetifier//:jetifier_standalone"),
            cfg = "host",
        ),
    },
    implementation = _jetify_impl,
)

# Literal and glob pattern matches for artifacts that should not have jetifier applied.
# These can be overridden in the main call using maven_repository_specification(jetifier_excludes=)
DEFAULT_JETIFIER_EXCLUDED_ARTIFACTS = [
    "javax.*:*",
    "*:jsr305",
    "*:javac-shaded",
    "*:google-java-format",
    "com.squareup:javapoet",
    "com.google.dagger:*",
    "org.bouncycastle*:*",
    "androidx*:*",
    "org.jetbrains.kotlin*:*",
    "com.android.tools:*",
    "com.android.tools.build:*",
]

def _prepare_jetifier_excludes(ctx):
    literal = []
    id_literal = []
    group_literal = []
    prefix_matches = []
    for exclude in ctx.attr.jetifier_excludes:
        (group_id, artifact_id) = exclude.split(":")
        if artifact_id != "*" and artifact_id.find("*") >= 0:
            fail((
                "Jetifier exclude %s may not include a partial wildcard in its artifact id. " +
                "An exclude artifact_id can only be a string literal or itself be a " +
                "wildcard. E.g.: \"foo:bar\", \"foo:*\". \"foo:ba*\" is not permitted."
            ) % exclude)
        group_wildcard_index = group_id.find("*")
        if group_id == "*":
            if artifact_id == "*":
                fail("*:* is not a valid exclusions match. Just set use_jetifier=False instead.")

            # e.g. "*:dagger"
            id_literal += [artifact_id]
        elif group_wildcard_index >= 0:
            if not group_id == "*" and not group_id.endswith("*"):
                fail((
                    "Jetifier exclude %s may not include a wildcard at the start or in the " +
                    "middle of the group_id. An exclude group_id can only be a string " +
                    "literal or itself be  a wildcard, or end with a wildcard. E.g.: " +
                    "\"foo.bar:baz\", \"foo.b*:baz\" or \"*:baz\". \"foo.b*r:baz\" is not " +
                    "permitted."
                ) % exclude)
            prefix_matches += [struct(
                prefix = group_id[0:group_wildcard_index],
                artifact_id = artifact_id,
            )]
        else:
            # group_id is a literal
            if artifact_id == "*":
                group_literal += [group_id]
            else:
                literal += [exclude]
    return struct(
        literal = sets.copy_of(literal),
        id_literal = sets.copy_of(id_literal),
        group_literal = sets.copy_of(group_literal),
        prefix_matches = prefix_matches,
    )

def _should_use_jetifier(coordinate, enabled, excludes):
    (group_id, artifact_id) = coordinate.split(":")
    should = (
        enabled and
        not sets.contains(excludes.literal, coordinate) and
        not sets.contains(excludes.group_literal, group_id) and
        not sets.contains(excludes.id_literal, artifact_id)
    )
    if should:  # why test more if it's already matched (i.e. already excluded)?
        for match in excludes.prefix_matches:
            if group_id.startswith(match.prefix):
                if match.artifact_id == "*" or match.artifact_id == artifact_id:
                    # Found a match, so we shouldn't do jetifier, so bail early.
                    should = False
                    break
    return should

jetify_utils = struct(
    should_use_jetifier = _should_use_jetifier,
    prepare_jetifier_excludes = _prepare_jetifier_excludes,
)
