# Description:
#   String variables containing build-file segment substitutes, to be used with the
#   `maven_repository_specification(build_substitutes = {...})` dictionary property.  These have specific documentation
#   on any template variables
#
#   Given the volatility (and lack of automation) of the deps lists in these substitutions, these templates aren't
#   intended for generic usage.  They can be freely cut-and-pasted as given in this example file, but the deps list
#   needs curation.  Indeed, it mostly only has variable substitution because the unversioned coordinates don't change
#   so frequently.

# Description:
#   Substitutes the naive maven_jvm_artifact for com.google.dagger:dagger with a wrapper that
#   exports the compiler plugin.  Contains the `version` substitution variable.
DAGGER_BUILD_SNIPPET_WITH_PLUGIN = """
java_library(
   name = "dagger",
   exports = [":dagger-api"],
   exported_plugins = [":dagger-plugin"],
   visibility = ["//visibility:public"],
)

# com.google.dagger:dagger:{version}
raw_jvm_import(
    name = "dagger-api",
    jar = "@com_google_dagger_dagger//maven",
    deps = [
       "@maven//javax/inject:javax_inject",
    ],
)

java_plugin(
   name = "dagger-plugin",
   processor_class = "dagger.internal.codegen.ComponentProcessor",
   generates_api = True,
   deps = [":dagger-compiler"],
)
"""

# Description:
#   Substitutes the naive maven_jvm_artifact for com.google.dagger:dagger with a wrapper that
#   exports the processor plugin.  Contains the `version` substitution variable.
#
#   This is similar to the dagger substitution snippet, but the organization between the API target
#   upon which one is to depend, and the plugin target is a bit different, and we want the resulting
#   visible target to be different.
#
#   Also this uses the older mvn_jvm_artifact mechanism, which is deprecated in favor of the more
#   explicit raw_jvm_import.
AUTO_VALUE_BUILD_SNIPPET_WITH_PLUGIN = """
java_library(
   name = "value",
   exports = [":auto-value-annotations"],
   exported_plugins = [":plugin"],
   visibility = ["//visibility:public"],
)

raw_jvm_import(
    name = "auto-value-processor",
    jar = "@com_google_auto_value_auto_value//maven",
)

java_plugin(
   name = "plugin",
   processor_class = "com.google.auto.value.processor.AutoValueProcessor",
   generates_api = True,
   deps = [":auto-value-processor"],
)
"""
