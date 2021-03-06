var utils = require('./utils')
var config = require('../config')
var isProduction = process.env.NODE_ENV === 'production'
/*引入postcss-px2rem 通过require的形式*/ 
// var px2rem = require('postcss-px2rem');

module.exports = {
    loaders: utils.cssLoaders({
        sourceMap: isProduction ?
            config.build.productionSourceMap : config.dev.cssSourceMap,
        extract: false//0705暂时修改为false
    })
    /*配置remUnit*/
    // postcss: function () {
    //     return [px2rem({
    //         remUnit: 100
    //     })];
    // }
}
