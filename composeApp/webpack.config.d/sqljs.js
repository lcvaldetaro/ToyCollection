// Emit the sql.js WebAssembly binary next to composeApp.js (sql.js loads it by file name at run time).
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.plugins = config.plugins || [];
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            { from: require.resolve('sql.js/dist/sql-wasm-browser.wasm'), to: '.' }
        ]
    })
);
