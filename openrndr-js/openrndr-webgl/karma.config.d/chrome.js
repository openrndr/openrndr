if (process.platform === 'darwin') {
    config.set({
        customLaunchers: {
            ChromeHeadlessWithWebGL: {
                base: 'Chrome',
                flags: [
                    '--headless=new',
                    '--use-gl=angle',
                    '--use-angle=swiftshader',
                    '--enable-webgl',
                    '--ignore-gpu-blocklist',
                    '--no-sandbox'
                ]
            }
        },
        browsers: ['ChromeHeadlessWithWebGL']
    });
}

if (process.env.GITHUB_ACTIONS) {
    config.set({
        customLaunchers: {
            ChromeHeadlessWithWebGL: {
                base: 'Chrome',
                flags: [
                    '--headless=new',
                    '--use-gl=angle',
                    '--use-angle=swiftshader',
                    '--enable-webgl',
                    '--ignore-gpu-blocklist',
                    '--no-sandbox',
                    '--disable-dev-shm-usage'
                ]
            }
        },
        browsers: ['ChromeHeadlessWithWebGL']
    });
}
