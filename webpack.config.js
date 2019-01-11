module.exports = {
    entry: {
        mediasoup_client: './src/js/mediasoup_client.js',
        protoo_client: './src/js/protoo_client.js'
    },
    output: {
        filename: '[name].js',
        path: __dirname + '/target/foreign_libs'
    }
};
