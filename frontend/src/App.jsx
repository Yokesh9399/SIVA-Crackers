import { BrowserRouter } from 'react-router-dom';
import { SettingsProvider } from './context/SettingsContext.jsx';
import { EstimateProvider } from './context/EstimateContext.jsx';
import AppRoutes from './routes/AppRoutes.jsx';

export default function App() {
  return (
    <BrowserRouter>
      <SettingsProvider>
        <EstimateProvider>
          <AppRoutes />
        </EstimateProvider>
      </SettingsProvider>
    </BrowserRouter>
  );
}
