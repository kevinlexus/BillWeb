/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.model.Org', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'cd', type: 'string' },
        { name: 'name', type: 'string' }
    ]
});