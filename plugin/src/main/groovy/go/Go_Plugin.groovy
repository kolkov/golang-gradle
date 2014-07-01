package go

apply plugin: 'groovy'
import org.gradle.api.file.*
import org.gradle.api.*
import org.gradle.api.tasks.*

// Default to user's GOPATH
//goPlug.goPath = "$System.env.GOPATH"
//goPlug.versionFile = "$System.env.GOPATH" + "/version.txt"
//goPlug.currentProject = "$System.env.PWD"
//goPlug.versionMap = [:]


class GoPlugin implements Plugin<Project> {
    void apply(Project project) {
        
        // DEFAULTS
        project.extensions.create("go",GoPluginExtension)
        project.defaultTasks 'goPlugin_Welcome','createVersionMap'

        // INFORMATIONAL TASKS

        project.task('goPlugin_Welcome') {
            doFirst{
                println "Welcome to the goPlugin, you're settings are as follows:"
                println "  Root Project: $project.rootProject"
                println "  At: $project.rootDir"
                println "  GoPath: $project.go.goPath"
                println "  Version File: $project.go.versionFile"
                println "  Current Project scope: $project.go.currentProject"
            }  
        }

        project.task('printProjectTree') << {
            //FileTree goWorkspace
            FileTree goWorkspace = project.fileTree(dir: ("$project.go.goPath"+"/src"))
            goWorkspace.include '**/.git'
            println "  Go Projects in this workspace:"
            goWorkspace.visit {gitproject ->
                def t = new File("$gitproject".replace('file ','').replace("'",''))
                def foundGoFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/^\.git$/){
                        foundGoFile = true
                    }
                    if (foundGoFile){
                        println "    $t"
                    }
                }
            }
        }

        project.task('printImportList') << {
            println project.go.importList
        }

        project.task('getImportList') << {
            project.go.importList.each { aDependency ->
                project.tasks["goGet_$aDependency"].execute()
            }
            
        }

        // INITIALIZATION TASKS

        project.task('findImports') << {
            def list = []
            
            project.task("goGetT",type: Exec){
                workingDir = project.projectDir
                commandLine 'go', 'get', '-t'
            }

            project.tasks["goGetT"].execute()

            project.task("goDeps",type: Exec){
                workingDir = project.projectDir
                commandLine 'go','list', "-f", /{{join .Deps "\n"}}/
                standardOutput = new ByteArrayOutputStream()
                ext.output = {
                    return standardOutput.toString()
                }
            }
            project.tasks["goDeps"].execute()
            def rawDeps = project.tasks["goDeps"].output()

            project.task("goTestDeps",type: Exec){
                workingDir = project.projectDir
                commandLine 'go','list', "-f", /{{join .TestImports "\n"}}/
                standardOutput = new ByteArrayOutputStream()
                ext.output = {
                    return standardOutput.toString()
                }
            }
            project.tasks["goTestDeps"].execute()
            def testDeps = project.tasks["goTestDeps"].output()
            def allDeps = rawDeps + testDeps
            testDeps.split().each{ aPackage ->      
                project.task("testGoDep${aPackage}",type: Exec){
                    workingDir = project.projectDir
                    commandLine 'go','list', "-f", /{{join .Deps "\n"}}/, /${aPackage}/
                    standardOutput = new ByteArrayOutputStream()
                    ext.output = {
                        return standardOutput.toString()
                    }
                }
                project.tasks["testGoDep${aPackage}"].execute()
                allDeps << project.tasks["testGoDep${aPackage}"].output()
            }
            
            project.go.importList = allDeps.split().sort() as Set
        }

        project.task('createVersionMap') << {
            def File vf
            vf = project.file(project.go.versionFile)
            // Tab delimited file with projectName<tab>gitSHA on each line
            // Example: github.com/smartystreets/goconvey/web/server/system    2124ee55e7c5737f5ea4a7744b58069c1499b8cc
            if (vf.isFile()){
                vf.eachLine{ sourceVersion ->
                    project.go.versionMap[sourceVersion.tokenize("\t")[0]] = sourceVersion.tokenize("\t")[1]
                }
            }
            println "The projects you wish versioned, with the git commits you want:"
            println project.go.versionMap
        }

        // ACTION TASKS

        project.task('executeCheckouts') << {
            project.go.versionMap.each{ projectToUpdate ->
                def folder = "$project.go.goPath"+"/src/"+"$projectToUpdate.key"
                def gitVersion = "$projectToUpdate.value"
                def newTask = "checkout_"+"$projectToUpdate.value"
                println "Checking out commit: $gitVersion, on project: $folder"
                project.task("checkout_$gitVersion",type: Exec){
                    def option = ''
                    workingDir = "$folder"
                    executable = 'git'
                    if (option == ''){
                        args "checkout", "$gitVersion"
                    }
                    else{
                        args "$option", "checkout", "$gitVersion"
                    }
                }
                project.tasks["checkout_$gitVersion"].execute()
            }
        }

        project.task('clean') << {
            def File goWorkspace
            goWorkspace = project.file(("$project.go.goPath"+"/src"))
            goWorkspace.eachDir {t ->
                def foundGoFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+\.go$/){
                        foundGoFile = true
                    }
                    if (foundGoFile){
                        project.tasks["goClean_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }

        project.task('run') << {
            def File goWorkspace
            goWorkspace = project.file(("$project.go.goPath"+"/src"))
            goWorkspace.eachDir {t ->
                def foundGoFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+\.go$/){
                        foundGoFile = true
                    }
                    if (foundGoFile){
                        project.tasks["goRun_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }

        project.task('build') << {
            def File goWorkspace
            goWorkspace = project.file(("$project.go.goPath"+"/src"))
            goWorkspace.eachDir {t ->
                def foundGoFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+\.go$/){
                        foundGoFile = true
                    }
                    if (foundGoFile){
                        project.tasks["goBuild_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }

        project.task('install') << {
            def File goWorkspace
            goWorkspace = project.file(("$project.go.goPath"+"/src"))
            goWorkspace.eachDir {t ->
                def foundGoFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+\.go$/){
                        foundGoFile = true
                    }
                    if (foundGoFile){
                        project.tasks["goInstall_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }
        
        project.task('test') << {
            FileTree goWorkspace
            goWorkspace = project.fileTree(dir: ("$project.go.goPath" + "/src"))
            //goWorkspace = project.fileTree(dir: ("$project.go.currentProject"))
            goWorkspace.include '**/*_test.go'
            goWorkspace.visit {test_file ->
                def t = new File("$test_file".replace('file ','').replace("'",''))
                def foundATestFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+test\.go$/){
                        foundATestFile = true
                    }
                    if (foundATestFile){
                        project.tasks["goTest_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }

        project.task('benchmark') << {
            FileTree goWorkspace
            goWorkspace = project.fileTree(dir: ("$project.go.goPath" + "/src"))
            //goWorkspace = project.fileTree(dir: ("$project.go.currentProject"))
            goWorkspace.include '**/*_test.go'
            goWorkspace.visit {test_file ->
                def t = new File("$test_file".replace('file ','').replace("'",''))
                def foundATestFile = false
                if (t.isDirectory()){
                    t.eachFileMatch(~/.+test\.go$/){
                        foundATestFile = true
                    }
                    if (foundATestFile){
                        project.tasks["goBenchmark_"+"$t.absolutePath"-"$project.go.goPath"-"/src/"].execute()
                    }
                }
            }
        }

        // RULES FOR SPECIAL CASE EXECUTION

        project.tasks.addRule("Pattern: goBenchmark_<ID>"){ String taskName ->
            if (taskName.startsWith("goBenchmark_")){
                project.task(taskName,type:Exec){
                    println "  Benchmarking $taskName"-"goBenchmark_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'test', '-bench=.', (taskName - 'goBenchmark_')
                }
            }
        }

        project.tasks.addRule("Pattern: goRun_<ID>"){ String taskName ->
            if (taskName.startsWith("goRun_")){
                project.task(taskName,type:Exec){
                    println "  Running $taskName"-"goRun_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'run', (taskName - 'goRun_')
                }
            }
        }
        
        project.tasks.addRule("Pattern: goClean_<ID>"){ String taskName ->
            if (taskName.startsWith("goClean_")){
                project.task(taskName,type:Exec){
                    println "  Cleaning $taskName"-"goClean_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'clean', (taskName - 'goClean_')
                }
            }
        }

        project.tasks.addRule("Pattern: goGet_<ID>"){ String taskName ->
            if (taskName.startsWith("goGet_")){
                project.task(taskName,type:Exec){
                    println "  Getting $taskName"-"goGet_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'get', (taskName - 'goGet_')
                }
            }
        }

        project.tasks.addRule("Pattern: goInstall_<ID>"){ String taskName ->
            if (taskName.startsWith("goInstall_")){
                project.task(taskName,type:Exec){
                    println "  Installing $taskName"-"goInstall_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'install', (taskName - 'goInstall_')
                }
            }
        }

        project.tasks.addRule("Pattern: goBuild_<ID>"){ String taskName ->
            if (taskName.startsWith("goBuild_")){
                project.task(taskName,type:Exec){
                    println "  Building $taskName"-"goBuild_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'build', (taskName - 'goBuild_')
                }
            }
        }

        project.tasks.addRule("Pattern: goRun_<ID>"){ String taskName ->
            if (taskName.startsWith("goRun_")){
                project.task(taskName,type:Exec){
                    println "  Running $taskName"-"goRun_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'run', (taskName - 'goRun_')
                }
            }
        }

        project.tasks.addRule("Pattern: goTest_<ID>"){ String taskName ->
            if (taskName.startsWith("goTest_")){
                project.task(taskName,type:Exec){
                    println "  Testing $taskName"-"goTest_"+" in workspace $project.go.goPath"
                    workingDir project.go.goPath
                    executable 'go'
                    args 'test', (taskName - 'goTest_')
                }
            }
        }

        project.task('prepareGoWorkspace',dependsOn: ['getImportList','executeCheckouts','install']) << {
            project.tasks['getImportList'].execute()
            project.tasks['executeCheckouts'].execute()
            project.tasks['install'].execute()
        }
        // TASK DEPENDENCIES

        
        project.createVersionMap.dependsOn project.goPlugin_Welcome
        project.executeCheckouts.dependsOn project.createVersionMap
        project.printImportList.dependsOn project.findImports
        project.getImportList.dependsOn project.findImports
        project.executeCheckouts.mustRunAfter project.getImportList
    
        
    }

}

/*
// Future-ism create goTool plugin for calling the gotools with any options we want
class goTool extends Exec {
    String projectPath = ''
    String subTool = ''
    String option = ''

    @TaskAction
    def void runGoTool(){
        workingDir = "$projectPath"
        executable = 'go'
        if (option == ''){
            args "$subTool"
        }
        else{
            args "$option", "subTool"
        }
        
    }
}*/