/**
 * Created by lev on 30.03.2017.
 */
Ext.define('BillWebApp.store.AddrTpSpec', {
    extend: 'Ext.data.Store',
    alias  : 'store.addrtpstore',
    storeId: 'AddrTpStore',
    model: 'BillWebApp.model.AddrTp',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : 'http://192.168.100.21:8083/base/getAddrTp',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    }
});