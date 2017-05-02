/**
 * Created by lev on 27.03.2017.
 */
Ext.define('BillWebApp.model.Area', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'cd', type: 'string' },
        { name: 'name', type: 'string' }
    ]
});