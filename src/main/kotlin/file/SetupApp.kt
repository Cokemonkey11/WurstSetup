package file

import config.WurstProjectConfig
import config.WurstProjectConfigData
import global.InstallationManager
import mu.KotlinLogging
import net.ConnectionManager
import ui.UiManager
import ui.UpdateFoundDialog
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*


object SetupApp {
	val DEFAULT_DIR = Paths.get(".")!!
    private val log = KotlinLogging.logger {}
    lateinit var setup: SetupMain

    fun handleArgs(setup: SetupMain) {
        this.setup = setup
        if (!setup.silent) {
            log.info("is GUI launch")
            UiManager.initUI()
        } else {
            log.info("is silent launch")
            handleCMD()
        }
        startup()
    }

    private fun handleCMD() {
		ConnectionManager.checkConnectivity()
		ConnectionManager.checkWurstBuild()
		InstallationManager.verifyInstallation()
		log.info("handle runargs")
		if (setup.projectDir != DEFAULT_DIR) {
			log.info("project dir exists")
			if (setup.generate) {
				log.info("is create project")
				WurstProjectConfig.handleCreate(setup.projectDir, null, WurstProjectConfigData())
			} else if (setup.update) {
				log.info("is update project")
				WurstProjectConfig.handleUpdate(setup.projectDir, null, WurstProjectConfig.loadProject(setup.projectDir.resolve("wurst.build"))!!)
			}
		} else if (setup.remove) {
            log.info("remove installation")
            if (setup.force) {
                InstallationManager.handleRemove()
            }
        } else if (setup.update) {
			log.info("updating")
            if (InstallationManager.status != InstallationManager.InstallationStatus.INSTALLED_UPTODATE) {
                log.info("compiler update eligible")
                if (setup.force) {
					log.info("Forcing update..")
                    InstallationManager.handleUpdate()
                } else {
					if (!setup.silent) {
						UpdateFoundDialog("A Wurst compiler update has been found!")
					} else {
						log.info("Do you want to update your wurst installation? (y/n)")
						val sc = Scanner(System.`in`)
						val line = sc.nextLine()
						if (line == "y") {
							InstallationManager.handleUpdate()
						}
					}
                }
            } else {
				log.info("Already up to date.")
			}
        }
    }

    private fun startup() {
        log.info("startup setup version: <{}>", CompileTimeInfo.version)
        InstallationManager.verifyInstallation()
        copyJar()
    }

    private fun copyJar() {
        val url = InstallationManager::class.java.protectionDomain.codeSource.location
        val ownFile = Paths.get(url.toURI())
        if (ownFile.endsWith(".2.jar")) {
            log.info("copy jar from own")
            Files.copy(ownFile, ownFile.resolveSibling("WurstSetup.jar"), StandardCopyOption.REPLACE_EXISTING)
        }
        log.info("path: $url")
        log.info("file: " + ownFile.toAbsolutePath())
        if (ownFile != null && Files.exists(ownFile) && ownFile.toString().endsWith(".jar") &&
                (ownFile.parent == null || ownFile.parent.fileName.toString() != ".wurst")) {
            log.info("copy jar")
            Files.copy(ownFile, Paths.get(InstallationManager.installDir.toString(), "WurstSetup.jar"), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
