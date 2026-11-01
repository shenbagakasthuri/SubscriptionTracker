import api from './axiosInstance';

export const getSubscriptions = async () => {
    const response = await api.get('/api/subscriptions');
    return response.data;
};

export const getSubscriptionById = async (id) => {
    const response = await api.get(`/api/subscriptions/${id}`);
    return response.data;
};

export const createSubscription = async (subscription) => {
    const response = await api.post('/api/subscriptions', subscription);
    return response.data;
};

export const updateSubscription = async (id, subscription) => {
    const response = await api.put(`/api/subscriptions/${id}`, subscription);
    return response.data;
};

export const deleteSubscription = async (id) => {
    const response = await api.delete(`/api/subscriptions/${id}`);
    return response;
};