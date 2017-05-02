/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.model.PayordGrp', {
    extend: 'Ext.data.Model',
    identifier: {
        type: 'sequential',
        id: 'foo'
    },
    idProperty: 'id',
    fields: [
        { name: 'id', mapping: 'id', type: 'int'},
        { name: 'name', type: 'string' },
        { name: 'dtf', dateFormat: 'Y-m-d H:i:s', type: 'date', persist: false },
        { name: 'username', type: 'string', persist: false }
    ]
});