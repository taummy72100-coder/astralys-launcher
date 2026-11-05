const { DistributionAPI } = require('helios-core/common')

const ConfigManager = require('./configmanager')

// Old WesterosCraft url.
// exports.REMOTE_DISTRO_URL = 'http://mc.westeroscraft.com/WesterosCraftLauncher/distribution.json'
exports.REMOTE_DISTRO_URL = 'https://raw.githubusercontent.com/taummy72100-coder/astralys-launcher/distribution/distribution.json'

// Apercu visuel : distribution locale, le bouton Jouer reste desactive.
exports.APERCU = process.env.ASTRALYS_APERCU === '1'
if(exports.APERCU){
    require('fs-extra').ensureDirSync(ConfigManager.getLauncherDirectory())
    require('fs-extra').copySync(require('path').join(__dirname, '..', 'distro-apercu.json'), require('path').join(ConfigManager.getLauncherDirectory(), 'distribution.json'))
}

const api = new DistributionAPI(
    ConfigManager.getLauncherDirectory(),
    null, // Injected forcefully by the preloader.
    null, // Injected forcefully by the preloader.
    exports.REMOTE_DISTRO_URL,
    false
)

exports.DistroAPI = api