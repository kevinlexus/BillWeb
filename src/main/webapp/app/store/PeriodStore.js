/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.PeriodStore1', {
    extend: 'Ext.data.Store',
    alias  : 'store.periodstore1',
    model: 'BillWebApp.model.Period',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : 'http://192.168.100.21:8083/rep/getPeriodReports',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }/*,
        extraParams :{
            repCd : 'RptPayDocList',
            tp: '0'
        }*/
    }
});