/**
 * Created by lev on 18.04.2017.
 */
Ext.define('BillWebApp.model.PayordFlow', {
    extend: 'Ext.data.Model',
    identifier: {
        type: 'sequential',
        id: 'foo'
    },
    idProperty: 'id',
    fields: [
        { name: 'id', mapping: 'id', type: 'int'},
        { name: 'payordFk', type: 'int'},
        { name: 'ukFk', type: 'int'},
        { name: 'summa', type: 'float', persist: true },
        { name: 'summa1', type: 'float', persist: false },
        { name: 'summa2', type: 'float', persist: false },
        { name: 'summa3', type: 'float', persist: false },
        { name: 'summa4', type: 'float', persist: false },
        { name: 'summa5', type: 'float', persist: false },
        { name: 'summa6', type: 'float', persist: false },
        { name: 'npp', type: 'int', persist: true},
        { name: 'signed', type: 'boolean', persist: true},
        { name: 'dt', dateFormat: 'Y-m-d H:i:s', type: 'date', persist: true},
        { name: 'period', type: 'string', persist: false}]
});