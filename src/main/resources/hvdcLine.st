for (equipment in network.<collectionName>) {
  <if(nominalV || nominalVMin || (countries1 && countries2))>if (<if(nominalV)>(equipment.nominalV <nominalVOperator> <nominalV>)<endif>
      <if(nominalVMin)>(equipment.nominalV >= <nominalVMin> && equipment.nominalV \<= <nominalVMax>)<endif>
      <if(nominalV && countries1 && countries2)>&& <endif><if(countries1)>(hvdcLineMatch(equipment, <countries1>, <countries2>))<endif>
     ) {
     <endif>
           contingency(equipment.id) { equipments equipment.id }
     <if(nominalV || nominalVMin || (countries1 && countries2))>
  }
   <endif>
}
