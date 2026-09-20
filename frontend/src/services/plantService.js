import { get, post, del } from './httpClient.js'

export const addPlant = ({ variety, comment }) => post('/plant', { variety, comment })

export const removePlant = (plantId) => del(`/plant/${plantId}`)

export const getAvailablePlants = () => get('/plant')
