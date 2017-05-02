/**
 * Created by lev on 30.03.2017.
 */
Ext.define('BillWebApp.model.AddrTp', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'cd', type: 'string' },
        { name: 'name', type: 'string' }
    ]
});