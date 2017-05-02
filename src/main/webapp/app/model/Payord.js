/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.model.Payord', {
    extend: 'Ext.data.Model',
    identifier: {
        type: 'sequential',
        id: 'foo'
    },
    idProperty: 'id',
    fields: [
        { name: 'id', mapping: 'id', type: 'int'},
        { name: 'name', type: 'string' },
        { name: 'periodTpFk', type: 'int', convert: null},
        { name: 'payordGrpFk', type: 'int', convert: null},
        { name: 'selDays', type: 'string' },
        { name: 'username', type: 'string', persist: false},
        { name: 'dtf', dateFormat: 'Y-m-d H:i:s', type: 'date', persist: false },
        { name: 'formula', type: 'string' }
    ]
});