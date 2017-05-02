/**
 * Created by lev on 18.04.2017.
 */
Ext.define('BillWebApp.store.PayordFlowStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.payordflowstore',
    storeId: 'payordflowstore',
    model: 'BillWebApp.model.PayordFlow',
    config:{// перенести в модель, если нужно autoSync = false
        autoLoad: true,
        autoSync: false
    },
    proxy: {
        type: 'ajax',
        api: {
            create  : '',
            create  : 'http://192.168.100.21:8083/payord/addPayordFlow',
            read    : 'http://192.168.100.21:8083/payord/getPayordFlowByTpDt',
            update  : 'http://192.168.100.21:8083/payord/setPayordFlow',
            destroy : 'http://192.168.100.21:8083/payord/delPayordFlow'
        },
        reader: {
            type: 'json'
        },
        writer: {
            type: 'json',
            allowSingle: false, //запретить по одному отправлять отправлять объекты в Json - только массивом![объект] - иначе трудно описывать в Restful
            writeAllFields: true  //писать весь объект в json
        },
        extraParams : {
            tp : '2',
            dt: null
        }
    }

});