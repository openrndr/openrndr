// harfbuzzjs, used for fonts and text shaping, imports Node's 'module' built-in in code that only runs in Node.js,
// it is not bundled for browsers
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, { module: false });
