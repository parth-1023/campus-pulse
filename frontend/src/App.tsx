import { useState, useEffect } from 'react';
import DeckGL from '@deck.gl/react';
import { GeoJsonLayer } from '@deck.gl/layers';
import { Map } from 'react-map-gl/maplibre';
import { useAuth } from 'react-oidc-context';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import axios from 'axios';

import 'maplibre-gl/dist/maplibre-gl.css';

const MapComponent = Map as any;

const INITIAL_VIEW_STATE = {
  longitude: 6.0459,
  latitude: 46.2344,
  zoom: 15.5,
  pitch: 50,
  bearing: 30,
  maxZoom: 20,
  minZoom: 12
};

const MAP_STYLE = 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json';

// Calculate the centroid of a building polygon
function getCentroid(coordinates: number[][][]) {
  const outerRing = coordinates[0];
  let sumLon = 0;
  let sumLat = 0;
  outerRing.forEach((coord) => {
    sumLon += coord[0];
    sumLat += coord[1];
  });
  return {
    lon: sumLon / outerRing.length,
    lat: sumLat / outerRing.length
  };
}

export default function App() {
  const auth = useAuth();
  const [viewState, setViewState] = useState<any>(INITIAL_VIEW_STATE);
  const [geojsonData, setGeojsonData] = useState<any>(null);

  const [activeIncident, setActiveIncident] = useState<any>(null);
  const [alertBuildings, setAlertBuildings] = useState<Set<string>>(new Set());
  const [historicalCount, setHistoricalCount] = useState<number>(0);

  // 1. Fetch GeoJSON buildings footprint data on mount
  useEffect(() => {
    fetch('/cern_buildings.geojson')
      .then((res) => res.json())
      .then((data) => setGeojsonData(data))
      .catch((err) => console.error("Error loading GeoJSON buildings:", err));
  }, []);

  // 2. Register Axios Authorization Header Interceptor
  useEffect(() => {
    if (!auth.isAuthenticated) return;

    // Intercept every outgoing Axios call to attach the Keycloak JWT Bearer Token
    const interceptorId = axios.interceptors.request.use(
      (config) => {
        const token = auth.user?.access_token;
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Eject interceptor on unmount to prevent leaks
    return () => {
      axios.interceptors.request.eject(interceptorId);
    };
  }, [auth.isAuthenticated, auth.user]);

  // 3. Load historical incidents from Supabase on successful SSO authentication
  useEffect(() => {
    if (!auth.isAuthenticated || !geojsonData) return;

    console.log("Fetching historical incidents from secure API...");
    axios.get('http://localhost:8081/api/incidents')
      .then((res) => {
        const incidents = res.data;
        setHistoricalCount(incidents.length);
        console.log(`Loaded ${incidents.length} historical incidents.`);

        const loadedAlertBuildings = new Set<string>();

        incidents.forEach((incident: any) => {
          // Locate coordinates inside the JTS Point object
          const location = incident.location;
          if (location && location.coordinates) {
            const lon = location.coordinates[0];
            const lat = location.coordinates[1];

            // Map coordinate to the closest building footprint
            let closestBuildingId = "";
            let minDistance = Infinity;

            geojsonData.features.forEach((feature: any) => {
              if (feature.geometry && feature.geometry.type === "Polygon") {
                const centroid = getCentroid(feature.geometry.coordinates);
                const distance = Math.sqrt(
                  Math.pow(centroid.lon - lon, 2) +
                  Math.pow(centroid.lat - lat, 2)
                );

                if (distance < minDistance) {
                  minDistance = distance;
                  closestBuildingId = feature.id;
                }
              }
            });

            if (closestBuildingId) {
              loadedAlertBuildings.add(closestBuildingId);
            }
          }
        });

        // Set initial color state
        setAlertBuildings(loadedAlertBuildings);
      })
      .catch((err) => console.error("Error loading historical incidents:", err));
  }, [auth.isAuthenticated, geojsonData]);

  // 4. WebSocket listener to capture live alerts
  useEffect(() => {
    if (!auth.isAuthenticated || !geojsonData) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8081/ws-campus'),
      reconnectDelay: 5000,

      onConnect: () => {
        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body);
          setActiveIncident(alert);

          let closestBuildingId = "";
          let minDistance = Infinity;

          geojsonData.features.forEach((feature: any) => {
            if (feature.geometry && feature.geometry.type === "Polygon") {
              const centroid = getCentroid(feature.geometry.coordinates);
              const distance = Math.sqrt(
                Math.pow(centroid.lon - alert.longitude, 2) +
                Math.pow(centroid.lat - alert.latitude, 2)
              );

              if (distance < minDistance) {
                minDistance = distance;
                closestBuildingId = feature.id;
              }
            }
          });

          if (closestBuildingId) {
            setAlertBuildings((prev) => {
              const next = new Set(prev);
              next.add(closestBuildingId);
              return next;
            });
          }
        });
      }
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [auth.isAuthenticated, geojsonData]);

  if (auth.isLoading) {
    return <div style={{ padding: '24px', color: '#64748b' }}>Establishing Keycloak SSO session...</div>;
  }

  if (auth.error) {
    return <div style={{ padding: '24px', color: '#ef4444' }}>OIDC Authentication Error: {auth.error.message}</div>;
  }

  if (!auth.isAuthenticated) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', background: '#0f172a' }}>
        <h1 className="dashboard-title" style={{ fontSize: '32px' }}>CampusPulse Secure Console</h1>
        <p style={{ color: '#94a3b8', marginBottom: '24px' }}>Please log in to view the smart campus 3D interface.</p>
        <button
          onClick={() => auth.signinRedirect()}
          style={{ padding: '12px 24px', background: '#3b82f6', border: 'none', borderRadius: '8px', color: '#fff', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          Secure SSO Login
        </button>
      </div>
    );
  }

  const layers = [
    new GeoJsonLayer({
      id: 'cern-buildings-layer',
      data: geojsonData,
      opacity: 0.8,
      stroked: true,
      filled: true,
      extruded: true,
      wireframe: true,

      getElevation: (f: any) => {
        const levels = f.properties && f.properties['building:levels'];
        if (levels) {
          return parseFloat(levels) * 4;
        }
        return 15;
      },

      getFillColor: (f: any) => {
        if (alertBuildings.has(f.id)) {
          return [239, 68, 68, 220]; // Red alert
        }
        return [148, 163, 184, 180]; // Sleek Slate
      },
      getLineColor: [255, 255, 255, 30],
      getLineWidth: 1,

      updateTriggers: {
        getFillColor: [Array.from(alertBuildings)]
      }
    })
  ];

  return (
    <div style={{ width: '100vw', height: '100vh', position: 'relative' }}>
      {/* Dashboard UI Overlay */}
      <div className="dashboard-overlay">
        <h1 className="dashboard-title">CampusPulse</h1>
        <p className="dashboard-desc">
          Event-driven 3D spatial dashboard. Secured via Keycloak OIDC & JWT endpoints.
        </p>

        {/* User profile controls */}
        <div style={{ marginTop: '12px', fontSize: '12px', color: '#94a3b8' }}>
          User: <strong>{auth.user?.profile.preferred_username}</strong> | Incidents: <strong>{historicalCount}</strong>
          <button
            onClick={() => auth.signoutRedirect()}
            style={{ marginLeft: '12px', background: 'transparent', border: 'none', color: '#ef4444', textDecoration: 'underline', cursor: 'pointer', fontSize: '12px' }}
          >
            Logout
          </button>
        </div>

        {/* Live WebSocket Alarm Monitoring panel */}
        <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
          <h3 style={{ margin: '0 0 8px 0', fontSize: '14px', color: '#f8fafc' }}>Live Incident Stream</h3>
          {activeIncident ? (
            <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.4)', padding: '12px', borderRadius: '6px', fontSize: '13px' }}>
              <strong style={{ color: '#ef4444' }}>ALARM: Elevated CO2</strong>
              <p style={{ margin: '4px 0 0 0', color: '#e2e8f0', fontSize: '12px' }}>{activeIncident.description}</p>
              <div style={{ marginTop: '6px', fontSize: '11px', color: '#94a3b8' }}>
                CO2: {activeIncident.co2Level} ppm | Lat: {activeIncident.latitude}
              </div>
            </div>
          ) : (
            <p style={{ fontStyle: 'italic', fontSize: '12px', color: '#64748b' }}>Waiting for edge sensor anomalies...</p>
          )}
        </div>
      </div>

      {/* deck.gl canvas */}
      <DeckGL
        viewState={viewState}
        onViewStateChange={(e: any) => setViewState(e.viewState)}
        controller={true}
        layers={layers}
      >
        <MapComponent reuseMaps mapStyle={MAP_STYLE} />
      </DeckGL>
    </div>
  );
}
