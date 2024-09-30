export const schemaEventGetTenders = {
  type: 'object',
  required: ['queryStringParameters'],
  properties: {
    queryStringParameters: {
      type: 'object',
      required: ['page', 'size'],
      properties: {
        from: {
          type: 'string'
        },
        to: {
          type: 'string'
        },
        page: {
          type: 'number'
        },
        size: {
          type: 'number'
        }
      }
    }
  }
}