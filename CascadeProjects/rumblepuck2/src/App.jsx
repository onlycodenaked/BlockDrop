import React from 'react'
import PhysicsSandbox from './components/PhysicsSandbox'
import './styles/main.css'
import './styles/field.css'

function App() {
  return (
    <div className="app">
      <header>
        <h1>RumblePuck Physics Sandbox</h1>
        <p>Test and validate physics parameters for the game</p>
      </header>
      <main>
        <PhysicsSandbox />
      </main>
      <footer>
        <p>RumblePuck - Physics-First Development Approach</p>
      </footer>
    </div>
  )
}

export default App
