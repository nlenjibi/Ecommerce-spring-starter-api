"use client";

import { populateData } from "@/lib/data-population";

const DataPopulationPage = () => {
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Data Population</h1>
      <p className="mb-4">
        Use this tool to populate the database with initial data from{" "}
        <code>public.json</code>.
      </p>
      <button
        onClick={populateData}
        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
      >
        Populate Data
      </button>
    </div>
  );
};

export default DataPopulationPage;
