/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.LstStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.lststore',
    storeId: 'LstStore',
    model: 'BillWebApp.model.Lst',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : 'http://192.168.100.21:8083/base/getLstByTp',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    }
});