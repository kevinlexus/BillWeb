/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.store.PayordCmpStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.payordcmpstore',
    storeId: 'payordcmpstore',
    model: 'BillWebApp.model.PayordCmp',
    config:{// перенести в модель, если нужно autoSync = false
        autoLoad: false,
        autoSync: false
    },
    proxy: {
        type: 'ajax',
        api: {
            create  : '',
            create  : 'http://192.168.100.21:8083/payord/addPayordCmp',
            read    : 'http://192.168.100.21:8083/payord/getPayordCmp',
            update  : 'http://192.168.100.21:8083/payord/setPayordCmp',
            destroy : 'http://192.168.100.21:8083/payord/delPayordCmp'
        },
        reader: {
            type: 'json'
        },
        writer: {
            type: 'json',
            allowSingle: false, //запретить по одному отправлять отправлять объекты в Json - только массивом![объект] - иначе трудно описывать в Restful
            writeAllFields: true  //писать весь объект в json
        }
    }

});