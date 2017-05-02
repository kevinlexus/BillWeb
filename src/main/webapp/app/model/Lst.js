/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.model.Lst', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'name', type: 'string' }
    ]
});