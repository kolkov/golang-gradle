package go

class GoPluginExtension {
    String goPath = './'
    String versionFile = ''
    String currentProject = ''
    Map versionMap = [:]
    LinkedHashSet importList = [] as Set
}
