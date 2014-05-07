package go

class GoPluginExtension {
    def String goPath = './'
    def String versionFile = ''
    def String currentProject = ''
    def Map versionMap = [:]
    def LinkedHashSet importList = [] as Set
}