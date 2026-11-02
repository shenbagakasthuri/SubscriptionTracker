
import api from './axiosInstance';

// Get all transactions for the authenticated user
export const getTransactions = async () => {
    const response = await api.get('/api/sms/imports');
    return response.data;
};

// Get a single transaction by ID
export const getTransactionById = async (id) => {
    const response = await api.get(`/api/sms/imports/${id}`);
    return response.data;
};

// Get subscription suggestions based on imported transactions
export const getSubscriptionSuggestions = async () => {
    const response = await api.get('/api/sms/subscription-suggestions');
    return response.data;
};

// Parse SMS text and return a transaction preview
// This does not save the transaction
export const parseSms = async (smsText) => {
    const response = await api.post('/api/sms/parse', {
        smsText: smsText.trim()
    });
    return response.data;
};

// Import parsed SMS as a transaction
// The backend processes and saves the transaction
export const importSms = async (smsText) => {
    const response = await api.post('/api/sms/import', {
        smsText: smsText.trim()
    });
    return response.data;
};

// Delete an imported transaction
export const deleteTransaction = async (id) => {
    const response = await api.delete(`/api/sms/imports/${id}`);
    return response.data;
};
