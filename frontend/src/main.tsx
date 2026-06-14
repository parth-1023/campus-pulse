import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AuthProvider } from 'react-oidc-context'
import './index.css'
import App from './App.tsx'

const oidcConfig = {
  authority: 'http://localhost:8080/realms/campuspulse',
  client_id: 'frontend-client',
  redirect_uri: 'http://localhost:5173',
  onSigninCallback: () => {
    // Clear URL parameters (tokens) from the address bar after successful authentication redirect
    window.history.replaceState({}, document.title, window.location.pathname);
  }
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AuthProvider {...oidcConfig}>
      <App />
    </AuthProvider>
  </StrictMode>,
)
