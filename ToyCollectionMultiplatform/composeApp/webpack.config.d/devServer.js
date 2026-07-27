if (config.devServer) {
    config.devServer.historyApiFallback = {
        index: '/toycollection.html'
    };
    config.devServer.static = config.devServer.static || {};
    config.devServer.static.index = 'toycollection.html';
}
