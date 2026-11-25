import { BrowserRouter } from 'react-router-dom';
import { SettingsProvider } from './context/SettingsContext.jsx';
import { EstimateProvider } from './context/EstimateContext.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import AppRoutes from './routes/AppRoutes.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <SettingsProvider>
        <AuthProvider>
          <EstimateProvider>
            <AppRoutes />
          </EstimateProvider>
        </AuthProvider>
      </SettingsProvider>
    </BrowserRouter>
  );
}
